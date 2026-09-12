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

  # Utiliza os Security Groups informados ou cria o Security Group local da Lambda
  security_group_ids = length(var.vpc_security_group_ids) > 0 ? var.vpc_security_group_ids : aws_security_group.lambda_sg[*].id

  # Configuração do OpenTelemetry via OTLP/HTTP (Porta 4318)
  otel_env_vars = var.adot_layer_arn != "" ? {
    AWS_LAMBDA_EXEC_WRAPPER     = "/opt/otel-handler"
    OTEL_EXPORTER_OTLP_ENDPOINT = var.otel_collector_endpoint
    OTEL_EXPORTER_OTLP_PROTOCOL = "http/protobuf"
    OTEL_SERVICE_NAME           = local.function_name
    OTEL_PROPAGATORS            = "tracecontext,baggage"
  } : {}

  # URL base da aplicação: utiliza dinamicamente o DNS do Load Balancer na porta 8080 caso encontrado, ou o fallback de var.app_base_url
  resolved_app_base_url = try(data.aws_lb.app_k8s[0].dns_name, "") != "" ? "http://${data.aws_lb.app_k8s[0].dns_name}:8080" : var.app_base_url
}

# -----------------------------------------------------------------------------
# Busca Dinâmica Segura na AWS: Localiza o Load Balancer criado pelo Kubernetes (EKS)
# Utiliza aws_lbs (plural) para não lançar erro fatal caso o EKS/LB já tenha sido destruído
# -----------------------------------------------------------------------------
data "aws_lbs" "app_k8s" {
  tags = {
    "kubernetes.io/service-name" = "${var.k8s_namespace}/${var.k8s_service_name}"
  }
}

data "aws_lb" "app_k8s" {
  count = var.use_dynamic_lb_lookup && length(data.aws_lbs.app_k8s.arns) > 0 ? 1 : 0
  arn   = tolist(data.aws_lbs.app_k8s.arns)[0]
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

# Security Group dedicado à Função Lambda quando em VPC
resource "aws_security_group" "lambda_sg" {
  count       = local.is_in_vpc && length(var.vpc_security_group_ids) == 0 && local.vpc_id != null && local.vpc_id != "" ? 1 : 0
  name        = "${local.function_name}-sg"
  description = "Security Group para a Lambda Auth do RepairShop (${var.environment})"
  vpc_id      = local.vpc_id

  egress {
    description = "Saida para VPC e Internet (acesso ao Postgres RDS, APIs internas e telemetria)"
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
      security_group_ids = local.security_group_ids
    }
  }

  environment {
    variables = merge(
      { APP_BASE_URL = local.resolved_app_base_url },
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
