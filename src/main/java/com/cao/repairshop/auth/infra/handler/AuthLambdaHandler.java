package com.cao.repairshop.auth.infra.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cao.repairshop.auth.application.usecase.AuthenticateUseCase;
import com.cao.repairshop.auth.application.usecase.impl.AuthenticateUseCaseImpl;
import com.cao.repairshop.auth.domain.exception.ValidationException;
import com.cao.repairshop.auth.domain.model.AuthToken;
import com.cao.repairshop.auth.domain.model.Credentials;
import com.cao.repairshop.auth.infra.adapter.AuthFeignAdapter;
import com.cao.repairshop.auth.infra.client.AuthClient;
import com.cao.repairshop.auth.infra.config.FeignConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador de Infraestrutura AWS Lambda (Primary Input Adapter).
 * Recebe e sanitiza eventos HTTP do API Gateway, orquestra o caso de uso
 * e retorna respostas formatadas com headers de segurança OWASP.
 */
public class AuthLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger log = LoggerFactory.getLogger(AuthLambdaHandler.class);

    private final AuthenticateUseCase authenticateUseCase;
    private final ObjectMapper objectMapper;

    public AuthLambdaHandler() {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String appBaseUrl = resolveAppBaseUrl();
        AuthClient authClient = FeignConfig.createAuthClient(appBaseUrl, this.objectMapper);
        AuthFeignAdapter feignAdapter = new AuthFeignAdapter(authClient);
        
        this.authenticateUseCase = new AuthenticateUseCaseImpl(feignAdapter);
    }

    public AuthLambdaHandler(AuthenticateUseCase authenticateUseCase, ObjectMapper objectMapper) {
        this.authenticateUseCase = authenticateUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> headers = createSecurityHeaders();

        if (input == null || input.getBody() == null || input.getBody().isBlank()) {
            return buildErrorResponse(400, "Bad Request", "O corpo da requisição não pode ser vazio.", headers);
        }

        try {
            CredentialsInput inputDto = objectMapper.readValue(input.getBody(), CredentialsInput.class);
            if (inputDto.getCpf() == null || inputDto.getCpf().isBlank()
                    || inputDto.getPassword() == null || inputDto.getPassword().isBlank()) {
                log.warn("⚠️ Payload de login incompleto: cpf ou password ausentes.");
                return buildErrorResponse(400, "Bad Request", "Os campos 'cpf' e 'password' são obrigatórios.", headers);
            }
            Credentials credentials = new Credentials(inputDto.getCpf(), inputDto.getPassword());

            log.info("🔑 Processando autenticação para o CPF: {}", credentials.getCpf());

            AuthToken authToken = authenticateUseCase.execute(credentials);

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("token", authToken.getToken());

            return buildResponse(200, objectMapper.writeValueAsString(responseMap), headers);

        } catch (ValidationException e) {
            log.warn("⚠️ Falha de validação defensiva: {}", e.getMessage());
            return buildErrorResponse(400, "Bad Request", e.getMessage(), headers);

        } catch (FeignException e) {
            int status = e.status() > 0 ? e.status() : 500;
            String feignBody = e.contentUTF8();
            log.error("❌ FeignException status {}: {} (detalhes: {})", status, feignBody.isBlank() ? "Sem resposta da API" : feignBody, e.getMessage());

            if (feignBody != null && !feignBody.isBlank() && feignBody.trim().startsWith("{")) {
                return buildResponse(status, feignBody, headers);
            }

            String message = status == 401 ? "Credenciais inválidas." : "Erro na comunicação com a API de autenticação.";
            return buildErrorResponse(status, status == 401 ? "Unauthorized" : "Bad Gateway", message, headers);

        } catch (Exception e) {
            log.error("❌ Erro não tratado na Lambda: {}", e.getMessage(), e);
            return buildErrorResponse(500, "Internal Server Error", "Ocorreu um erro interno ao processar a autenticação.", headers);
        }
    }

    private String resolveAppBaseUrl() {
        String appBaseUrl = System.getenv("APP_BASE_URL");
        if (appBaseUrl == null || appBaseUrl.isBlank()) {
            appBaseUrl = System.getProperty("APP_BASE_URL");
        }

        // Se estiver rodando em container Docker local (SAM CLI), "app.repairshop.local" não resolve no DNS local do Docker.
        // O container precisa usar "host.docker.internal" para alcançar a porta 8081 da máquina host.
        boolean isLocalSamDocker = new File("/.dockerenv").exists()
                && (System.getenv("AWS_SAM_LOCAL") != null || System.getenv("LAMBDA_TASK_ROOT") != null)
                && (appBaseUrl == null || appBaseUrl.isBlank() || appBaseUrl.contains("repairshop.local"));

        if (isLocalSamDocker) {
            appBaseUrl = "http://host.docker.internal:8081";
        } else if (appBaseUrl == null || appBaseUrl.isBlank()) {
            appBaseUrl = "http://localhost:8081";
        }

        if (appBaseUrl.endsWith("/")) {
            appBaseUrl = appBaseUrl.substring(0, appBaseUrl.length() - 1);
        }
        log.info("🌐 Target Base URL: {}", appBaseUrl);
        return appBaseUrl;
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body, Map<String, String> headers) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(body);
    }

    private APIGatewayProxyResponseEvent buildErrorResponse(int statusCode, String error, String message, Map<String, String> headers) {
        try {
            Map<String, Object> errorPayload = new HashMap<>();
            errorPayload.put("timestamp", Instant.now().toString());
            errorPayload.put("status", statusCode);
            errorPayload.put("error", error);
            errorPayload.put("message", message);

            return buildResponse(statusCode, objectMapper.writeValueAsString(errorPayload), headers);
        } catch (Exception e) {
            return buildResponse(statusCode, String.format("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}", statusCode, error, message), headers);
        }
    }

    private Map<String, String> createSecurityHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers", "Content-Type,Authorization");
        headers.put("Access-Control-Allow-Methods", "POST,OPTIONS");
        // OWASP Security Headers
        headers.put("X-Content-Type-Options", "nosniff");
        headers.put("X-Frame-Options", "DENY");
        headers.put("Cache-Control", "no-store, max-age=0");
        headers.put("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        return headers;
    }

    // DTO interno exclusivo para desserialização do payload de entrada da Lambda
    private static class CredentialsInput {
        private String cpf;
        private String password;

        public String getCpf() { return cpf; }
        public void setCpf(String cpf) { this.cpf = cpf; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
