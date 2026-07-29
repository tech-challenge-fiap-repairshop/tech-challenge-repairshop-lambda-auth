aws_region                = "us-east-1"
environment               = "prd"
use_remote_network_state  = true
use_remote_rds_state      = true
use_vpc                   = true
lambda_memory_size        = 1024
lambda_timeout            = 30
lambda_runtime            = "provided.al2023"

# Telemetria & Observabilidade OpenTelemetry
adot_layer_arn            = "arn:aws:lambda:us-east-1:901920570421:layer:aws-otel-java-wrapper-amd64-ver-1-32-0:1"
otel_collector_endpoint   = "http://otel-collector.repairshop.local:4317"

# As credenciais e secrets sensíveis (db_username, db_password, jwt_private_key, jwt_public_key, etc.)
# são injetadas dinamicamente pelo GitHub Actions via Secrets/Variables do ambiente 'prd'.
