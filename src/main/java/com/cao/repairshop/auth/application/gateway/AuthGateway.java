package com.cao.repairshop.auth.application.gateway;

import com.cao.repairshop.auth.domain.model.AuthToken;
import com.cao.repairshop.auth.domain.model.Credentials;

/**
 * Porta de saída (Output Port) da arquitetura limpa para autenticação de credenciais.
 */
public interface AuthGateway {
    
    AuthToken authenticate(Credentials credentials);
}
