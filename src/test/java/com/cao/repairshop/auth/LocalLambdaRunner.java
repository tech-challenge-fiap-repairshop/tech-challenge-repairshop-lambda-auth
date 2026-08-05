package com.cao.repairshop.auth;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cao.repairshop.auth.infra.handler.AuthLambdaHandler;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

/**
 * Runner local para executar a AWS Lambda Auth no IntelliJ IDEA
 * utilizando o evento oficial do API Gateway Proxy (src/test/resources/event.json).
 */
public class LocalLambdaRunner {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("🚀 INICIANDO EXECUÇÃO LOCAL DA AWS LAMBDA AUTH");
        System.out.println("==================================================");

        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        APIGatewayProxyRequestEvent requestEvent;

        try (InputStream inputStream = LocalLambdaRunner.class.getResourceAsStream("/event.json")) {
            if (inputStream != null) {
                requestEvent = mapper.readValue(inputStream, APIGatewayProxyRequestEvent.class);
                System.out.println("✅ Evento carregado com sucesso do arquivo src/test/resources/event.json");
            } else {
                System.out.println("⚠️  Arquivo event.json não encontrado. Criando evento padrão em memória...");
                requestEvent = createFallbackEvent();
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao ler event.json: " + e.getMessage() + ". Utilizando evento padrão...");
            requestEvent = createFallbackEvent();
        }

        System.out.println("📌 Payload Enviado na Requisição:");
        System.out.println(requestEvent.getBody());
        System.out.println("--------------------------------------------------");

        AuthLambdaHandler handler = new AuthLambdaHandler();
        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, null);

        System.out.println("\n📥 RESPOSTA DA LAMBDA:");
        System.out.println("Status Code: " + responseEvent.getStatusCode());
        System.out.println("Headers:     " + responseEvent.getHeaders());
        System.out.println("Body:        " + responseEvent.getBody());
        System.out.println("==================================================");
    }

    private static APIGatewayProxyRequestEvent createFallbackEvent() {
        return new APIGatewayProxyRequestEvent()
                .withPath("/auth/login")
                .withHttpMethod("POST")
                .withBody("""
                        {
                            "cpf": "52998224725",
                            "password": "secretpassword"
                        }
                        """);
    }
}
