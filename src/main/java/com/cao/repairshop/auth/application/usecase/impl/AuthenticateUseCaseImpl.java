package com.cao.repairshop.auth.application.usecase.impl;

import com.cao.repairshop.auth.application.gateway.AuthGateway;
import com.cao.repairshop.auth.application.usecase.AuthenticateUseCase;
import com.cao.repairshop.auth.domain.model.AuthToken;
import com.cao.repairshop.auth.domain.model.Credentials;
import com.cao.repairshop.auth.infra.validator.InputValidator;

/**
 * Implementação do Caso de Uso de Autenticação.
 * Valida rigorosamente os dados de entrada e delega para o AuthGateway.
 */
public class AuthenticateUseCaseImpl implements AuthenticateUseCase {

    private final AuthGateway authGateway;

    public AuthenticateUseCaseImpl(AuthGateway authGateway) {
        this.authGateway = authGateway;
    }

    @Override
    public AuthToken execute(Credentials credentials) {
        InputValidator.validateCredentials(credentials);
        return authGateway.authenticate(credentials);
    }
}
