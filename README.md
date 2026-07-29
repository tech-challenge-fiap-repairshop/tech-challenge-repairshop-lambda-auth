# tech-challenge-repairshop-lambda-auth

Repositório de **Autenticação Serverless (AWS Lambda)** da arquitetura RepairShop (FIAP Tech Challenge - Fase 3).

## 🚀 Tecnologias

- **Java 24**
- **Quarkus 3.19.2**
- **Quarkus Amazon Lambda HTTP** (`quarkus-amazon-lambda-http`)
- **Quarkus Hibernate ORM Panache & PostgreSQL**
- **SmallRye JWT** (`quarkus-smallrye-jwt`)
- **Jakarta Bean Validation** (Anotações customizadas `@VerifyDocument` e `@VerifyEmail`)
- **AWS RDS Proxy** (Multiplexação de Conexões de Banco Serverless)
- **OpenTelemetry (ADOT Lambda Layer)**
- **Terraform >= 1.5.0** (Infraestrutura como Código)
- **GitHub Actions** (Esteira CI/CD)

---

## 🛢️ Arquitetura de Conexão via AWS RDS Proxy

Conforme as melhores práticas da AWS para aplicações Serverless/Lambda conectadas ao PostgreSQL RDS:

### 1. **Separação de Infraestrutura & Ciclo de Vida**:
- O **AWS RDS Proxy** possui um **endpoint fixo e permanente** (ex: `repairshop-proxy.proxy-xyz.us-east-1.rds.amazonaws.com`).
- O RDS Proxy é provisionado **junto com a infraestrutura do Banco de Dados RDS**, pois é um recurso compartilhado que atende tanto a **Lambda Auth** quanto a aplicação principal **RepairShop App**.

### 2. **Consumo Automático via Terraform Remote State**:
- A função Lambda Auth não se conecta diretamente à porta bruta do PostgreSQL. Em seu lugar, ela se conecta ao RDS Proxy (`use_remote_rds_state = true`).
- O nosso Terraform em [infra/main.tf](file:///c:/Users/Alexandre-AGAMIN/Projetos-%20FIAP/github-organizations-projects/tech-challenge-repairshop-lambda-auth/infra/main.tf) lê o endpoint do RDS Proxy automaticamente a partir do estado remoto do banco no S3 (`rds/${environment}.tfstate`):
  ```hcl
  DB_URL = "jdbc:postgresql://${local.rds_proxy_host}:5432/repairshop"
  ```
- O RDS Proxy reutiliza o pool de conexões e evita a exaustão de conexões no banco (`too many connections`) quando centenas de instâncias concorrentes da Lambda sobem na nuvem.

---

## ☕ Suporte a Java 24 no AWS Lambda

O AWS Lambda oferece runtimes gerenciados padrão até o `java21`. Para utilizar o **Java 24** no Lambda com Quarkus:
- Utilizamos o **Custom Runtime** (`runtime = "provided.al2023"`), onde o Quarkus é empacotado como um executável/runner otimizado.
- Opcionalmente, pode ser gerado um binário nativo GraalVM (`mvn package -Dnative`), obtendo tempo de inicialização (*Cold Start*) de dezenas de milissegundos no runtime `provided.al2023`.

---

## 🔑 Estratégia de Chaves JWT (Dev Local vs Produção AWS)

1. **Desenvolvimento Local e Testes (`mvn test`)**:
   - Os arquivos `src/main/resources/privateKey.pem` e `publicKey.pem` são chaves de teste estáticas mantidas na sua máquina local (e ignoradas no `.gitignore`).

2. **Produção na AWS (GitHub Secrets)**:
   - Cadastre as secrets no GitHub Repository/Environment (**Settings -> Environments -> `hml` / `prd`**):
     - `JWT_PRIVATE_KEY`: Conteúdo PEM da chave privada real de produção.
     - `JWT_PUBLIC_KEY`: Conteúdo PEM da chave pública real de produção.
     - `DB_USERNAME`: Usuário do banco PostgreSQL / RDS Proxy.
     - `DB_PASSWORD`: Senha do banco PostgreSQL / RDS Proxy.
   - O pipeline do GitHub Actions injeta estas secrets no Terraform via variáveis `TF_VAR_*`, que por sua vez configura as variáveis de ambiente na AWS Lambda.

---

## 📊 Observabilidade com OpenTelemetry (ADOT)

O CloudWatch de aplicação não é utilizado para telemetria nesta Lambda. Em seu lugar, deixamos configurada a **AWS Distro for OpenTelemetry (ADOT) Lambda Layer**:
- **Layer ARN**: `arn:aws:lambda:${var.aws_region}:901920570421:layer:aws-otel-java-wrapper-amd64-ver-1-32-0:1`
- **Wrappers & Variáveis**:
  - `AWS_LAMBDA_EXEC_WRAPPER = "/opt/otel-handler"`
  - `OTEL_EXPORTER_OTLP_ENDPOINT = "http://otel-collector.repairshop.local:4317"`
  - `OTEL_SERVICE_NAME = "repairshop-lambda-auth-${environment}"`

---

## 📌 Endpoints

| Método | Rota             | Descrição                                 |
| ------ | ---------------- | ----------------------------------------- |
| `GET`  | `/auth/health`   | Health Check do Lambda                    |
| `POST` | `/auth/login`    | Autenticação do cliente por CPF e emissão de JWT |
| `POST` | `/auth/register` | Cadastro do cliente e emissão de JWT      |

---

## 🏗️ Infraestrutura Terraform (`infra/`)

A infraestrutura do Lambda está desacoplada e segregada por ambientes (`dev`, `hml`, `prd`).

### Backend S3 (`infra/backend.tf`):
- **Bucket S3**: `fiap-repairshop2`
- **Key**: `terraform-config/lambda-auth-tfstate/${environment}/terraform.tfstate`
- **Region**: `us-east-1`

---

## 🤖 Esteira de CI/CD (GitHub Actions)

O arquivo `.github/workflows/ci-cd-lambda.yml` está dividido em **2 Jobs independentes**:

1. **`build` (Build & Test)**: Compila o projeto com JDK 24 e executa a suíte de testes do Quarkus.
2. **`terraform` (Provision Infrastructure)**:
   - Depende do sucesso do job `build` (`needs: build`).
   - Carrega dinamicamente o ambiente (`hml` ou `prd`).
   - Mapeia automaticamente as Secrets (`JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`, `DB_USERNAME`, `DB_PASSWORD`) e Variables (`JWT_ISSUER`, `RDS_PROXY_ENDPOINT`) via `TF_VAR_*`.
   - Garante a existência do Bucket S3 `fiap-repairshop2`.
   - Inicializa o Terraform no backend S3 `terraform-config/lambda-auth-tfstate/${ENV}/terraform.tfstate`.
   - Executa `terraform plan` (em PRs e pushes) e `terraform apply` (exclusivamente na branch `main` ou em disparos manuais).
