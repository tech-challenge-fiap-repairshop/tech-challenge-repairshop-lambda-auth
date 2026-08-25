# tech-challenge-repairshop-lambda-auth

Repositório de **Autenticação Serverless (AWS Lambda)** da arquitetura RepairShop (FIAP Tech Challenge - Fase 3).

## 🚀 Tecnologias

- **Java 21** (Managed Runtime nativo do AWS Lambda: `java21`)
- **AWS Lambda Java Core & Events** (`com.amazonaws:aws-lambda-java-core`, `aws-lambda-java-events`)
- **OpenFeign** (`feign-core`, `feign-jackson`) para chamadas HTTP
- **Jackson Databind** para serialização / desserialização JSON
- **Terraform >= 1.5.0** (Infraestrutura como Código)
- **GitHub Actions** (Esteira CI/CD)

---

## 🛢️ Arquitetura e Integração via Feign Client

Nesta nova versão, a Lambda de Autenticação foi desacoplada de conexões com banco de dados PostgreSQL/RDS.
- A função Lambda recebe o evento clássico do **API Gateway Proxy** (`APIGatewayProxyRequestEvent`).
- O handler (`AuthLambdaHandler`) extrai o payload de login (`email` e `password`) e utiliza o **OpenFeign Client** (`AuthClient`) para realizar a chamada HTTP para o endpoint `POST /auth/login` da aplicação principal (`tech-challenge-repairshop-app`).
- O token JWT (ou resposta de erro) retornado pela aplicação principal é repassado ao cliente do API Gateway via `APIGatewayProxyResponseEvent`.

---

## 📌 Configuração de Ambiente

A URL base da aplicação principal pode ser configurada via variável de ambiente:
- **`APP_BASE_URL`**: URL da API principal (ex: `http://app.repairshop.local:8080` ou em dev local `http://host.docker.internal:8080`).

---

## 🏗️ Compilação e Empacotamento

O projeto utiliza o **`maven-shade-plugin`** para gerar o arquivo JAR executável completo (fat-jar):

```bash
mvn clean package
```

O artefato final é gerado em `target/function.jar`, pronto para deploy no runtime `java21` da AWS Lambda.

---

## 🤖 Esteira de CI/CD (GitHub Actions)

O pipeline `.github/workflows/ci-cd-lambda.yml` compila o projeto em Java 21, executa a suíte de testes unitários e realiza o provisionamento/deploy via Terraform.
