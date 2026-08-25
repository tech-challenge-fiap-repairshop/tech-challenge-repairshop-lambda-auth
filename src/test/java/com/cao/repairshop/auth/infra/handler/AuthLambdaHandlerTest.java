package com.cao.repairshop.auth.infra.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cao.repairshop.auth.application.usecase.AuthenticateUseCase;
import com.cao.repairshop.auth.domain.exception.ValidationException;
import com.cao.repairshop.auth.domain.model.AuthToken;
import com.cao.repairshop.auth.domain.model.Credentials;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthLambdaHandlerTest {

    private AuthenticateUseCase mockAuthenticateUseCase;
    private ObjectMapper objectMapper;
    private AuthLambdaHandler handler;
    private Context mockContext;

    @BeforeEach
    void setUp() {
        mockAuthenticateUseCase = mock(AuthenticateUseCase.class);
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        handler = new AuthLambdaHandler(mockAuthenticateUseCase, objectMapper);
        mockContext = mock(Context.class);
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 e Token com Headers OWASP quando autenticar com sucesso")
    void shouldReturn200AndTokenWhenAuthenticationSucceeds() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withBody("{\"cpf\":\"52998224725\",\"password\":\"secretpassword\"}");

        AuthToken authToken = new AuthToken("mocked-jwt-token");
        when(mockAuthenticateUseCase.execute(any(Credentials.class))).thenReturn(authToken);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("mocked-jwt-token"));
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertEquals("nosniff", response.getHeaders().get("X-Content-Type-Options"));
        assertEquals("DENY", response.getHeaders().get("X-Frame-Options"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 com erro formatado quando a validação falhar")
    void shouldReturn400WhenValidationFails() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withBody("{\"cpf\":\"11111111111\",\"password\":\"123\"}");

        when(mockAuthenticateUseCase.execute(any(Credentials.class)))
                .thenThrow(new ValidationException("O formato do CPF informado é inválido."));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("O formato do CPF informado é inválido."));
    }

    @Test
    @DisplayName("Deve retornar HTTP 401 quando o backend retornar 401 Unauthorized")
    void shouldReturn401WhenBackendReturnsUnauthorized() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withBody("{\"cpf\":\"52998224725\",\"password\":\"wrongpassword\"}");

        Request feignReq = Request.create(Request.HttpMethod.POST, "/auth/login", Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        Response feignResp = Response.builder()
                .status(401)
                .reason("Unauthorized")
                .request(feignReq)
                .body("{\"error\":\"Credenciais inválidas\"}", StandardCharsets.UTF_8)
                .build();

        when(mockAuthenticateUseCase.execute(any(Credentials.class)))
                .thenThrow(FeignException.errorStatus("login", feignResp));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertNotNull(response);
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Credenciais inválidas"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 500 sem expor detalhes internos em caso de exceção inesperada")
    void shouldReturn500WithoutLeakingInternalDetailsOnUnexpectedError() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withBody("{\"cpf\":\"52998224725\",\"password\":\"secretpassword\"}");

        when(mockAuthenticateUseCase.execute(any(Credentials.class)))
                .thenThrow(new RuntimeException("Banco inacessível"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertNotNull(response);
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Ocorreu um erro interno ao processar a autenticação."));
        assertFalse(response.getBody().contains("Banco inacessível"));
    }
}
