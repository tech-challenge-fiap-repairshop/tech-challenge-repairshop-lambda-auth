# Busca dinâmica da conta AWS atual para montagem da LabRole
data "aws_caller_identity" "current" {}

locals {
  function_name   = "repairshop-lambda-auth-${var.environment}"
  lambda_zip_path = "${path.module}/../target/function.jar"

  # Utiliza a mesma LabRole da infraestrutura do app principal (AWS Academy / IAM Role Padrão)
  lab_role_arn = var.lab_role_arn != "" ? var.lab_role_arn : "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/LabRole"

  is_in_vpc  = var.use_vpc || var.use_remote_network_state
  vpc_id     = var.use_remote_network_state ? try(data.terraform_remote_state.network[0].outputs.vpc_id, var.vpc_id) : var.vpc_id
  subnet_ids = var.use_remote_network_state ? try(data.terraform_remote_state.network[0].outputs.private_subnet_ids, var.vpc_subnet_ids) : var.vpc_subnet_ids

  # Configuração do OpenTelemetry via OTLP/HTTP (Porta 4318)
  otel_env_vars = var.adot_layer_arn != "" ? {
    AWS_LAMBDA_EXEC_WRAPPER     = "/opt/otel-handler"
    OTEL_EXPORTER_OTLP_ENDPOINT = var.otel_collector_endpoint
    OTEL_EXPORTER_OTLP_PROTOCOL = "http/protobuf"
    OTEL_SERVICE_NAME           = local.function_name
    OTEL_PROPAGATORS            = "tracecontext,baggage"
  } : {}
}

# Referência ao estado remoto da Infraestrutura de Rede Base (infra-network)
data "terraform_remote_state" "network" {
  count   = var.use_remote_network_state ? 1 : 0
  backend = "s3"

  config = {
    bucket = var.s3_tfstate_bucket
    key    = var.remote_state_network_key != "" ? var.remote_state_network_key : "network/${var.environment}.tfstate"
    region = var.aws_region
  }
}

# Security Group da Função Lambda (quando em VPC)
resource "aws_security_group" "lambda_sg" {
  count       = local.is_in_vpc && local.vpc_id != null && local.vpc_id != "" ? 1 : 0
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

# Função AWS Lambda Java 21 utilizando a mesma LabRole da infra do app
resource "aws_lambda_function" "auth_lambda" {
  function_name = local.function_name
  role          = local.lab_role_arn
  handler       = "com.cao.repairshop.auth.infra.handler.AuthLambdaHandler::handleRequest"
  runtime       = var.lambda_runtime
  memory_size   = var.lambda_memory_size
  timeout       = var.lambda_timeout

  filename         = local.lambda_zip_path
  source_code_hash = fileexists(local.lambda_zip_path) ? filebase64sha256(local.lambda_zip_path) : null

  layers = compact([var.adot_layer_arn])

  dynamic "vpc_config" {
    for_each = local.is_in_vpc && length(local.subnet_ids) > 0 ? [1] : []
    content {
      subnet_ids         = local.subnet_ids
      security_group_ids = compact(concat(aws_security_group.lambda_sg[*].id, var.vpc_security_group_ids))
    }
  }

  environment {
    variables = merge(
      { APP_BASE_URL = var.app_base_url },
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
