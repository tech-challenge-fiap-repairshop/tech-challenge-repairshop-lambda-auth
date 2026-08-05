package com.cao.repairshop.auth.domain.model;

import lombok.Value;

/**
 * Objeto de valor imutável do domínio representando credenciais de acesso validadas.
 */
@Value
public class Credentials {
    String cpf;
    String password;

    public Credentials(String cpf, String password) {
        this.cpf = cpf != null ? cpf.trim() : null;
        this.password = password;
    }
}
