package com.cao.repairshop.auth.domain.model;

import lombok.Value;

/**
 * Objeto de valor imutável do domínio representando o token de autenticação emitido.
 */
@Value
public class AuthToken {
    String token;

    public AuthToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token não pode ser nulo ou vazio");
        }
        this.token = token;
    }
}
