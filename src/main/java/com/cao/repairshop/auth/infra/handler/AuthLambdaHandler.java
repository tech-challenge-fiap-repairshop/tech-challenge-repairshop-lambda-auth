package com.cao.repairshop.auth.infra.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cao.repairshop.auth.infra.client.AuthClient;
import com.cao.repairshop.auth.infra.client.dto.AuthTokenResponseDTO;
import com.cao.repairshop.auth.infra.client.dto.LoginRequestDTO;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.FeignException;
import feign.Logger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AuthLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AuthClient authClient;
    private final ObjectMapper objectMapper;

    public AuthLambdaHandler() {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String appBaseUrl = System.getenv("APP_BASE_URL");
        if (appBaseUrl == null || appBaseUrl.isBlank()) {
            appBaseUrl = System.getProperty("APP_BASE_URL");
        }
        if (appBaseUrl == null || appBaseUrl.isBlank()) {
            appBaseUrl = resolveDefaultAppBaseUrl();
        }
        if (appBaseUrl.endsWith("/")) {
            appBaseUrl = appBaseUrl.substring(0, appBaseUrl.length() - 1);
        }

        System.out.println("🌐 Initializing AuthClient targeting APP_BASE_URL: " + appBaseUrl);

        this.authClient = Feign.builder()
                .encoder(new JacksonEncoder(objectMapper))
                .decoder(new JacksonDecoder(objectMapper))
                .logger(new Slf4jLogger(AuthClient.class))
                .logLevel(Logger.Level.BASIC)
                .target(AuthClient.class, appBaseUrl);
    }

    public AuthLambdaHandler(AuthClient authClient, ObjectMapper objectMapper) {
        this.authClient = authClient;
        this.objectMapper = objectMapper;
    }

    private String resolveDefaultAppBaseUrl() {
        // Se estiver rodando dentro de um Container Docker (ex: AWS SAM CLI / AWS Lambda Container)
        boolean isInsideDocker = new File("/.dockerenv").exists() 
                || System.getenv("AWS_LAMBDA_FUNCTION_NAME") != null 
                || System.getenv("LAMBDA_TASK_ROOT") != null;

        if (isInsideDocker) {
            return "http://host.docker.internal:8081";
        } else {
            return "http://localhost:8081";
        }
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> headers = createDefaultHeaders();

        if (input == null || input.getBody() == null || input.getBody().isBlank()) {
            return buildResponse(400, "{\"error\": \"Corpo da requisição não pode ser vazio\"}", headers);
        }

        try {
            LoginRequestDTO loginRequest = objectMapper.readValue(input.getBody(), LoginRequestDTO.class);
            System.out.println("🔑 Invoking /auth/login for email: " + loginRequest.getEmail());

            AuthTokenResponseDTO tokenResponse = authClient.login(loginRequest);

            String responseBody = objectMapper.writeValueAsString(tokenResponse);
            return buildResponse(200, responseBody, headers);

        } catch (FeignException e) {
            int status = e.status() > 0 ? e.status() : 500;
            String errorMsg = e.contentUTF8();
            System.err.println(String.format("❌ FeignException status %d: %s", status, errorMsg));
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = String.format("{\"error\": \"Falha na autenticação (HTTP %d): %s\"}", status, e.getMessage());
            }
            return buildResponse(status, errorMsg, headers);
        } catch (Exception e) {
            System.err.println("❌ Internal Error: " + e.getMessage());
            String errorMsg = String.format("{\"error\": \"Erro interno no servidor: %s\"}", e.getMessage());
            return buildResponse(500, errorMsg, headers);
        }
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body, Map<String, String> headers) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(body);
    }

    private Map<String, String> createDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers", "Content-Type,Authorization");
        headers.put("Access-Control-Allow-Methods", "POST,OPTIONS");
        return headers;
    }
}
