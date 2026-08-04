package com.cao.repairshop.auth.domain.model;

import lombok.Value;

/**
 * Objeto de valor imutável do domínio representando credenciais de acesso validadas.
 */
@Value
public class Credentials {
    String email;
    String password;

    public Credentials(String email, String password) {
        this.email = email != null ? email.trim().toLowerCase() : null;
        this.password = password;
    }
}
