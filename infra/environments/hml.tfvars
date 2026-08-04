aws_region               = "us-east-1"
environment              = "hml"
use_remote_network_state = true
use_vpc                  = true
lambda_memory_size       = 512
lambda_timeout           = 30
lambda_runtime           = "java21"
app_base_url             = "http://hml-app.repairshop.local:8080"

# Telemetria & Observabilidade OpenTelemetry (OTLP/HTTP na porta 4318)
adot_layer_arn           = "arn:aws:lambda:us-east-1:901920570421:layer:aws-otel-java-wrapper-amd64-ver-1-32-0:1"
otel_collector_endpoint  = "http://otel-collector.repairshop.local:4318"
