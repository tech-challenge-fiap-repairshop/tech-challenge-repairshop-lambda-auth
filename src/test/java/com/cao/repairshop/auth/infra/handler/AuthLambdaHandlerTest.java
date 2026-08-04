package com.cao.repairshop.auth.infra.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cao.repairshop.auth.infra.client.AuthClient;
import com.cao.repairshop.auth.infra.client.dto.AuthTokenResponseDTO;
import com.cao.repairshop.auth.infra.client.dto.LoginRequestDTO;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthLambdaHandlerTest {

    private AuthClient mockAuthClient;
    private ObjectMapper objectMapper;
    private AuthLambdaHandler handler;
    private Context mockContext;

    @BeforeEach
    void setUp() {
        mockAuthClient = mock(AuthClient.class);
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        handler = new AuthLambdaHandler(mockAuthClient, objectMapper);
        mockContext = mock(Context.class);
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 e Token quando as credenciais forem válidas")
    void shouldReturn200AndTokenWhenCredentialsAreValid() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withBody("{\"email\":\"carlos@repairshop.com\",\"password\":\"secretpassword\"}");

        AuthTokenResponseDTO expectedResponse = new AuthTokenResponseDTO("mocked-jwt-token");
        when(mockAuthClient.login(any(LoginRequestDTO.class))).thenReturn(expectedResponse);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("mocked-jwt-token"));
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 quando o corpo da requisição estiver vazio")
    void shouldReturn400WhenBodyIsEmpty() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withBody("");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("não pode ser vazio"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 401 quando o Feign retornar não autorizado (401)")
    void shouldReturn401WhenFeignReturnsUnauthorized() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withBody("{\"email\":\"carlos@repairshop.com\",\"password\":\"wrongpassword\"}");

        Request feignReq = Request.create(Request.HttpMethod.POST, "/auth/login", Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        Response feignResp = Response.builder()
                .status(401)
                .reason("Unauthorized")
                .request(feignReq)
                .body("{\"error\":\"Credenciais inválidas\"}", StandardCharsets.UTF_8)
                .build();

        when(mockAuthClient.login(any(LoginRequestDTO.class))).thenThrow(FeignException.errorStatus("login", feignResp));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertNotNull(response);
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Credenciais inválidas"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 500 quando ocorrer uma exceção genérica")
    void shouldReturn500OnGenericException() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withBody("{\"email\":\"carlos@repairshop.com\",\"password\":\"secretpassword\"}");

        when(mockAuthClient.login(any(LoginRequestDTO.class))).thenThrow(new RuntimeException("Conexão recusada"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertNotNull(response);
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Erro interno no servidor"));
    }
}
