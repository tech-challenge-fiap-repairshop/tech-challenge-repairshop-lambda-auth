package com.cao.repairshop.auth.application.usecase;

import com.cao.repairshop.auth.application.gateway.AuthGateway;
import com.cao.repairshop.auth.application.usecase.impl.AuthenticateUseCaseImpl;
import com.cao.repairshop.auth.domain.exception.ValidationException;
import com.cao.repairshop.auth.domain.model.AuthToken;
import com.cao.repairshop.auth.domain.model.Credentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthenticateUseCaseTest {

    private AuthGateway mockAuthGateway;
    private AuthenticateUseCase useCase;

    @BeforeEach
    void setUp() {
        mockAuthGateway = mock(AuthGateway.class);
        useCase = new AuthenticateUseCaseImpl(mockAuthGateway);
    }

    @Test
    @DisplayName("Deve autenticar com sucesso quando as credenciais forem válidas")
    void shouldAuthenticateSuccessfullyWhenCredentialsAreValid() {
        Credentials credentials = new Credentials("carlos@repairshop.com", "secret123");
        AuthToken expectedToken = new AuthToken("valid-jwt-token");

        when(mockAuthGateway.authenticate(any(Credentials.class))).thenReturn(expectedToken);

        AuthToken actualToken = useCase.execute(credentials);

        assertNotNull(actualToken);
        assertEquals("valid-jwt-token", actualToken.getToken());
        verify(mockAuthGateway, times(1)).authenticate(credentials);
    }

    @Test
    @DisplayName("Deve falhar na validação sem acionar a porta AuthGateway se o e-mail for inválido")
    void shouldFailValidationWithoutCallingGatewayWhenEmailIsInvalid() {
        Credentials credentials = new Credentials("email-invalido", "secret123");

        assertThrows(ValidationException.class, () -> useCase.execute(credentials));
        verify(mockAuthGateway, never()).authenticate(any());
    }
}
