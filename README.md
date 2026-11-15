# ⚡🔑 Repair Shop — Lambda Auth (`lambda-auth`)

[![CI/CD Pipeline](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-lambda-auth/actions/workflows/ci-cd-lambda.yml/badge.svg)](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-lambda-auth/actions/workflows/ci-cd-lambda.yml)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![AWS Lambda](https://img.shields.io/badge/AWS-Lambda%20(Java%2021)-FF9900?logo=awslambda&logoColor=white)](https://aws.amazon.com/lambda/)
[![Terraform](https://img.shields.io/badge/Terraform-1.8.5-7B42BC?logo=terraform&logoColor=white)](https://www.terraform.io/)

Função **AWS Lambda em Java 21** seguindo **Clean Architecture** responsável por interceptar e processar o fluxo de login (`POST /auth/login`) na borda da plataforma **Repair Shop (Tech Challenge FIAP — 15SOAT)**. Realiza validação defensiva de CPF/payload, aplica headers de segurança **OWASP**, delega a autenticação via **OpenFeign** (`AuthClient`) para o Network Load Balancer interno do `app-register-service` no EKS e suporta telemetria OpenTelemetry (ADOT Layer).

---

## 📋 Índice

- [Arquitetura da Função e Segurança](#arquitetura-da-função-e-segurança)
- [Arquitetura Atual — Motivação e Benefícios dos Componentes](#arquitetura-atual--motivação-e-benefícios-dos-componentes)
- [Por que DynamoDB em vez de Banco Relacional nos Contextos de OS e Pagamentos?](#por-que-dynamodb-em-vez-de-banco-relacional-nos-contextos-de-os-e-pagamentos)
- [Como Executar e Testar Localmente (Maven + Terraform)](#como-executar-e-testar-localmente-maven--terraform)
- [Como Executar em Produção (Esteira CI/CD GitHub Actions)](#como-executar-em-produção-esteira-cicd-github-actions)
- [Documentação Técnica e Collection do Postman](#documentação-técnica-e-collection-do-postman)

---

## Arquitetura da Função e Segurança

- **Core em Clean Architecture (Java 21)**:
  - **`AuthLambdaHandler`**: Handler HTTP compatível com payload `AWS_PROXY` (API Gateway HTTP API v2), devolvendo respostas JSON padronizadas com cabeçalhos defensivos recomendados pela OWASP (`X-Content-Type-Options`, `X-Frame-Options`, `Cache-Control`, entre outros).
  - **`InputValidator`**: Valida formato e consistência de CPF/entrada antes mesmo de invocar o backend na VPC, mitigando tráfego inválido logo na borda *serverless*.
  - **`AuthenticateUseCase` & OpenFeign (`AuthClient`)**: Desacopla a regra de caso de uso do transporte HTTP para o `register-service`.
- **Integração Dinâmica VPC + NLB ([`infra/main.tf`](infra/main.tf))**:
  - Nos ambientes `hml` e `prd` (`use_vpc = true`, `use_remote_network_state = true`), o Terraform conecta a Lambda às sub-redes privadas da VPC (`network/${ENV}.tfstate`) e resolve automaticamente o Network Load Balancer interno do serviço de autenticação pela tag `kubernetes.io/service-name`.
  - Parametrização padrão (`dev`/`hml`/`prd`): runtime **`java21`**, **`512 MB`** de memória e timeout de **`30s`**.
- **Suporte a Telemetria OpenTelemetry (ADOT)**: O módulo Terraform declara suporte à variável `adot_layer_arn` (`/opt/otel-handler` -> OTLP HTTP `:4318`) e `otel_collector_endpoint` *(atualmente com `adot_layer_arn = ""` nos arquivos `.tfvars` para manter compatibilidade com permissões da sandbox AWS Academy)*.

---

## Arquitetura Atual — Motivação e Benefícios dos Componentes

| Pilar / Tecnologia | Papel no Ecossistema | Motivação Técnica e Pontos Positivos |
| :--- | :--- | :--- |
| **RDS (PostgreSQL 16)** | Persistência de usuários (`tb_user`) no [`app-register-service`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-app-register-service) via [`infra-db-rds`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-infra-db-rds) | Concentra credenciais com hash BCrypt e CPF único (`UNIQUE`) em banco ACID isolado em sub-rede privada, sem exigir que a Lambda abra conexões JDBC diretamente do ambiente *serverless*. |
| **DynamoDB (NoSQL)** | Persistência de OS e Pagamentos ([`infra-db-dynamo`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-infra-db-dynamo)) | Atende os serviços posteriores ao login (`service-order` e `payment`) com Single-Table Design, `PAY_PER_REQUEST` e zero saturação de conexões sob alta concorrência. |
| **ElastiCache (Redis 7.0)** | Cache em memória ([`infra-redis`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-infra-redis)) | Provisionado em sub-rede privada (`6379`) para suportar evolução do `register-service` com cache sub-milissegundo de sessão/token para picos posteriores à validação feita nesta Lambda. |
| **Apps (Lambda Auth + EKS)** | **Borda Serverless (`lambda-auth`)** + **Microsserviços EKS (`1.31`)** | Separa a autenticação pontual em **modelo pay-per-use com elasticidade instantânea (*scale-to-zero*)** da computação contínua dos microsserviços em **EKS** com HPA. |
| **Amazon SQS (Mensageria + DLQ)** | Orquestração assíncrona ([`infra-sqs`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-infra-sqs)) | Após o usuário obter o JWT via Lambda Auth, aprovações de OS, notificações e pagamentos fluem assincronamente em 5 filas SQS + 5 DLQs em Saga Coreografada. |
| **API Gateway (HTTP API v2)** | Integração direta `POST /auth/login` ([`infra-apigateway`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-infra-apigateway)) | Encaminha requisições `POST /auth/login` diretamente à Lambda via `AWS_PROXY` com baixa latência e suporte CORS aos headers W3C (`traceparent`, `tracestate`). |
| **Observabilidade (Grafana)** | Dashboard dedicado `repairshop-lambda-auth.json` + OTel/Jaeger/Prometheus/Loki | Permite acompanhar invocação, latência, falhas e propagação de traces distribuídos entre Lambda, NLB e microsserviços no Grafana. |

---

## Por que DynamoDB em vez de Banco Relacional nos Contextos de OS e Pagamentos?

Embora a autenticação utilize dados cadastrais relacionais no **RDS (`tb_user`)** atrás do `register-service`, os serviços consumidos logo após o login (**OS** e **Pagamentos**) empregam **DynamoDB**:

1. **Prevenção de esgotamento de conexões em cargas variáveis**: Clientes autenticados disparam rajadas paralelas de criação/execução de OS e webhooks de pagamento no EKS (HPA). O DynamoDB via HTTPS *stateless* elimina a contenção de conexões TCP/JDBC inevitável em instâncias RDS `db.t3.micro`.
2. **Modelagem de agregado em Single-Table Design**: Uma OS (`ORDER#<id>`) agrupa metadados, histórico de 12 estados e tarefas extras sem `JOIN`s caros e com atomicidade via `TransactWriteItems`; faturamentos (`BILLING#<id>`) unem cobrança e fatura sem *locks* de `ALTER TABLE`.
3. **Consultas assíncronas eficientes**: GSIs específicos (`pending-dispatch-index`, `dispatch-notification-index`) alimentam fluxos assíncronos sem custo de `Full Table Scan`.

---

## Como Executar e Testar Localmente (Maven + Terraform)

> **Nota sobre build Maven**: Este repositório **não inclui Maven Wrapper** (`mvnw`). Utilize **Maven 3.9+** (`mvn`) com **JDK 21**.

### Pré-requisitos
- **JDK 21** e **Apache Maven 3.9+** (`mvn`)
- **Terraform `1.8.5`** e **AWS CLI v2** *(para validar a infraestrutura Terraform)*

### 1. Build e Testes Unitários do Pacote Lambda

```bash
# Executar testes unitários (JUnit 5 + Mockito)
mvn clean test

# Gerar o shaded JAR (fat JAR) em target/ para deploy na AWS Lambda
mvn clean package
```

### 2. Validação Local da Infraestrutura Terraform

```bash
cd infra

# Validar sintaxe localmente sem backend S3
terraform init -backend=false
terraform fmt -check -recursive
terraform validate

# Para planejar contra uma conta AWS real (requer state prévio da VPC/network em hml/prd)
terraform init \
  -backend-config="bucket=SEU_BUCKET_TERRAFORM_STATE" \
  -backend-config="key=lambda-auth/dev.tfstate" \
  -backend-config="region=us-east-1"
terraform plan -var-file="environments/dev.tfvars"
```

---

## Como Executar em Produção (Esteira CI/CD GitHub Actions)

O build Java e o provisionamento AWS são orquestrados pelo workflow [`.github/workflows/ci-cd-lambda.yml`](.github/workflows/ci-cd-lambda.yml) (e destruição em [`.github/workflows/destroy.yml`](.github/workflows/destroy.yml)).

### Mapeamento de Ambientes
- `develop` -> **`dev`** (`infra/environments/dev.tfvars`)
- `homolog` -> **`hml`** (`infra/environments/hml.tfvars`)
- `main` -> **`prd`** (`infra/environments/prd.tfvars`)

### Etapas da Esteira (`ci-cd-lambda.yml`)
1. Configura **JDK 21** e empacota o artefato Lambda com `mvn clean package`.
2. Executa `terraform fmt -check`, `terraform init` apontando para o bucket S3 de state remoto (`AWS_TERRAFORM_STATE_BUCKET`) e `terraform validate`.
3. Executa `terraform plan -var-file="environments/${ENV}.tfvars"`.
4. Executa `terraform apply -auto-approve` em push para `develop`, `homolog` e `main`, publicando/atualizando a função `repairshop-lambda-auth-${ENV}`.

> **Posição na Ordem de Provisionamento**: Provisionado após **`infra-network`** e **`infra-eks`/`app-register-service`** (quando `use_vpc = true`, para descoberta do NLB interno) e **antes** de **`infra-apigateway`**.

---

## Documentação Técnica e Collection do Postman

- **Infraestrutura e Build neste Repositório**: [`pom.xml`](pom.xml) | [`infra/main.tf`](infra/main.tf) | [`infra/environments/dev.tfvars`](infra/environments/dev.tfvars) | [`infra/environments/prd.tfvars`](infra/environments/prd.tfvars)
- **Dashboard Grafana de Lambda Auth**: [`repairshop-lambda-auth.json`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-app-service-order-execution-service/blob/main/local/observability/grafana/dashboards/repairshop-lambda-auth.json)
- **Diagrama de Sequência (Auth & Abertura de OS)**: [`diagrama_sequencia_auth_abertura_os.drawio.png`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-app-service-order-execution-service/blob/main/docs/delivery/sequence/diagrama_sequencia_auth_abertura_os.drawio.png)
- **ADRs e RFCs**:
  - [ADRs (`ADR-001` a `ADR-013`)](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-app-service-order-execution-service/blob/main/ADRs/README.md)
  - [RFCs (`RFC-001` a `RFC-014`)](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-app-service-order-execution-service/blob/main/RFCs/README.md)
- **Postman Collections & Environments (Organização)**:
  - Collection Principal: [`Repair Shop (Tech Challenge FIAP)`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-app-service-order-execution-service/blob/main/docs/postman/Repair%20Shop%20%28Tech%20Challenge%20FIAP%29.postman_collection.json)
  - Collection Completa: [`Tech Challenge FIAP - Completo`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-app-service-order-execution-service/blob/main/docs/postman/Tech_Challenge_Fiap_-_Completo.postman_collection.json)
  - Environment Local: [`Local.postman_environment.json`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-app-service-order-execution-service/blob/main/docs/postman/Local.postman_environment.json)
  - Guia do Postman: [`docs/postman/README.md`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-app-service-order-execution-service/blob/main/docs/postman/README.md)
- **Repositórios relacionados**: [`app-register-service`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-app-register-service) | [`infra-apigateway`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-infra-apigateway) | [`infra-network`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-repairshop-infra-network)
