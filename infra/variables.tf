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

variable "remote_state_network_key" {
  description = "Caminho da chave da state de rede infra-network no S3 (ex: network/dev.tfstate)"
  type        = string
  default     = ""
}

variable "use_remote_network_state" {
  description = "Se true, lê os dados de VPC e Subnets do estado remoto da rede base (infra-network)"
  type        = bool
  default     = false
}

variable "use_vpc" {
  description = "Define se a função Lambda deve ser executada dentro da VPC"
  type        = bool
  default     = false
}

variable "vpc_id" {
  description = "ID da VPC (utilizado caso não esteja usando remote state)"
  type        = string
  default     = ""
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

variable "lab_role_arn" {
  description = "ARN da IAM Role (LabRole da AWS Academy). Se vazio, é construído dinamicamente a partir do account_id"
  type        = string
  default     = ""
}

variable "app_base_url" {
  description = "URL base da API tech-challenge-repairshop-app para ser chamada via Feign Client na Cloud (Porta 8080)"
  type        = string
  default     = "http://app.repairshop.local:8080"
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
  description = "Runtime da AWS Lambda (java21 para Java 21 Managed Runtime)"
  type        = string
  default     = "java21"
}

variable "adot_layer_arn" {
  description = "ARN da AWS Distro for OpenTelemetry (ADOT) Lambda Layer"
  type        = string
  default     = ""
}

variable "otel_collector_endpoint" {
  description = "Endpoint do OTel Collector Gateway para envio via OTLP/HTTP (Porta 4318)"
  type        = string
  default     = "http://otel-collector.repairshop.local:4318"
}
