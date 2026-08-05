package com.cao.repairshop.auth.infra.adapter;

import com.cao.repairshop.auth.application.gateway.AuthGateway;
import com.cao.repairshop.auth.domain.model.AuthToken;
import com.cao.repairshop.auth.domain.model.Credentials;
import com.cao.repairshop.auth.infra.client.AuthClient;
import com.cao.repairshop.auth.infra.client.dto.AuthTokenResponseDTO;
import com.cao.repairshop.auth.infra.client.dto.LoginRequestDTO;

/**
 * Adaptador de infraestrutura implementando a porta AuthGateway utilizando OpenFeign.
 */
public class AuthFeignAdapter implements AuthGateway {

    private final AuthClient authClient;

    public AuthFeignAdapter(AuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    public AuthToken authenticate(Credentials credentials) {
        LoginRequestDTO requestDTO = new LoginRequestDTO(credentials.getCpf(), credentials.getPassword());
        
        AuthTokenResponseDTO responseDTO = authClient.login(requestDTO);

        return new AuthToken(responseDTO.getToken());
    }
}
