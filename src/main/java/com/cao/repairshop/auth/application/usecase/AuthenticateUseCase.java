package com.cao.repairshop.auth.application.usecase;

import com.cao.repairshop.auth.domain.model.AuthToken;
import com.cao.repairshop.auth.domain.model.Credentials;

/**
 * Caso de Uso (Input Port) para execução da autenticação de usuários.
 */
public interface AuthenticateUseCase {

    AuthToken execute(Credentials credentials);
}
