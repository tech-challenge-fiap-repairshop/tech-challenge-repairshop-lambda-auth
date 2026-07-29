variable "aws_region" {
  description = "Região da AWS para deploy dos recursos"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Ambiente de implantação (dev, hml, prd)"
  type        = string
  default     = "dev"
}

variable "s3_tfstate_bucket" {
  description = "Nome do Bucket S3 centralizado para controle de estado do Terraform"
  type        = string
  default     = "fiap-repairshop2"
}

variable "use_remote_network_state" {
  description = "Se true, lê os dados de VPC e Subnets do estado remote da rede base (network/ENV.tfstate)"
  type        = bool
  default     = false
}

variable "use_remote_rds_state" {
  description = "Se true, lê os dados de endpoint do RDS Proxy/PostgreSQL do estado remote do banco (rds/ENV.tfstate)"
  type        = bool
  default     = false
}

variable "use_vpc" {
  description = "Define se a função Lambda deve ser executada dentro da VPC"
  type        = bool
  default     = false
}

variable "vpc_subnet_ids" {
  description = "Lista de IDs das Subnets para associar a Lambda se use_vpc=true e sem remote state"
  type        = list(string)
  default     = []
}

variable "vpc_security_group_ids" {
  description = "Lista de Security Group IDs adicionais para a Lambda"
  type        = list(string)
  default     = []
}

variable "jwt_issuer" {
  description = "Issuer dos tokens JWT gerados pela Lambda de autenticação"
  type        = string
  default     = "https://repairshop.auth.com"
}

variable "jwt_lifespan_seconds" {
  description = "Tempo de expiração dos tokens JWT em segundos"
  type        = number
  default     = 86400
}

variable "jwt_private_key" {
  description = "Conteúdo PEM da chave privada RSA para assinatura JWT (injetado via Secret do GitHub Actions)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "jwt_public_key" {
  description = "Conteúdo PEM da chave pública RSA para validação JWT (injetado via Secret do GitHub Actions)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "rds_proxy_endpoint" {
  description = "Endpoint direto do AWS RDS Proxy (ex: rds-proxy-auth.cluster-xyz.us-east-1.rds.amazonaws.com)"
  type        = string
  default     = ""
}

variable "db_url" {
  description = "URL completa do banco de dados PostgreSQL RDS / RDS Proxy"
  type        = string
  default     = ""
}

variable "db_username" {
  description = "Usuário do banco de dados PostgreSQL RDS"
  type        = string
  default     = "repairshop"
  sensitive   = true
}

variable "db_password" {
  description = "Senha do banco de dados PostgreSQL RDS"
  type        = string
  default     = ""
  sensitive   = true
}

variable "lambda_memory_size" {
  description = "Quantidade de memória alocada para a função Lambda em MB"
  type        = number
  default     = 512
}

variable "lambda_timeout" {
  description = "Timeout da função Lambda em segundos"
  type        = number
  default     = 30
}

variable "lambda_runtime" {
  description = "Runtime da AWS Lambda (java21 para Managed Runtime ou provided.al2023 para Custom Runtime/Java 24/Quarkus Native)"
  type        = string
  default     = "provided.al2023"
}

variable "adot_layer_arn" {
  description = "ARN da AWS Distro for OpenTelemetry (ADOT) Lambda Layer"
  type        = string
  default     = ""
}

variable "otel_collector_endpoint" {
  description = "Endpoint do OTel Collector Gateway para envio de métricas e traces (OTLP)"
  type        = string
  default     = "http://otel-collector.repairshop.local:4317"
}
