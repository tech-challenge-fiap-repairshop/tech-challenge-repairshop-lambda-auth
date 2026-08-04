locals {
  function_name   = "repairshop-lambda-auth-${var.environment}"
  lambda_zip_path = "${path.module}/../target/function.jar"

  is_in_vpc  = var.use_vpc || var.use_remote_network_state
  vpc_id     = var.use_remote_network_state ? try(data.terraform_remote_state.network[0].outputs.vpc_id, null) : null
  subnet_ids = var.use_remote_network_state ? try(data.terraform_remote_state.network[0].outputs.private_subnet_ids, var.vpc_subnet_ids) : var.vpc_subnet_ids

  # Variáveis de ambiente dinâmicas para OpenTelemetry (ADOT Layer)
  otel_env_vars = var.adot_layer_arn != "" ? {
    AWS_LAMBDA_EXEC_WRAPPER     = "/opt/otel-handler"
    OTEL_EXPORTER_OTLP_ENDPOINT = var.otel_collector_endpoint
    OTEL_SERVICE_NAME           = local.function_name
    OTEL_PROPAGATORS            = "tracecontext,baggage"
  } : {}
}

# Referência opcional ao estado da Infraestrutura de Rede Base (VPC, Subnets, etc)
data "terraform_remote_state" "network" {
  count   = var.use_remote_network_state ? 1 : 0
  backend = "s3"

  config = {
    bucket = var.s3_tfstate_bucket
    key    = "network/${var.environment}.tfstate"
    region = var.aws_region
  }
}

# IAM Role para Execução da AWS Lambda
resource "aws_iam_role" "lambda_exec" {
  name = "${local.function_name}-exec-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

# Política para Execução em VPC (se habilitado)
resource "aws_iam_role_policy_attachment" "lambda_vpc_execution" {
  count      = local.is_in_vpc ? 1 : 0
  role       = aws_iam_role.lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

# Security Group da Função Lambda (quando em VPC)
resource "aws_security_group" "lambda_sg" {
  count       = local.is_in_vpc && local.vpc_id != null ? 1 : 0
  name        = "${local.function_name}-sg"
  description = "Security Group para a Lambda Auth do RepairShop (${var.environment})"
  vpc_id      = local.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.function_name}-sg"
  }
}

# Função AWS Lambda Java 21 com Feign Client
resource "aws_lambda_function" "auth_lambda" {
  function_name = local.function_name
  role          = aws_iam_role.lambda_exec.arn
  handler       = "com.cao.repairshop.auth.infra.handler.AuthLambdaHandler::handleRequest"
  runtime       = var.lambda_runtime
  memory_size   = var.lambda_memory_size
  timeout       = var.lambda_timeout

  filename         = local.lambda_zip_path
  source_code_hash = fileexists(local.lambda_zip_path) ? filebase64sha256(local.lambda_zip_path) : null

  # ADOT Lambda Layer para Coleta de Telemetria OpenTelemetry (opcional)
  layers = compact([var.adot_layer_arn])

  dynamic "vpc_config" {
    for_each = local.is_in_vpc && length(local.subnet_ids) > 0 ? [1] : []
    content {
      subnet_ids = local.subnet_ids
      security_group_ids = concat(
        aws_security_group.lambda_sg[*].id,
        var.vpc_security_group_ids
      )
    }
  }

  environment {
    variables = merge(
      {
        APP_BASE_URL = var.app_base_url
      },
      local.otel_env_vars
    )
  }
}

# Permissão para Invocação da Lambda pelo API Gateway
resource "aws_lambda_permission" "apigw_invoke" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.auth_lambda.function_name
  principal     = "apigateway.amazonaws.com"
}
