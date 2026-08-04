package com.cao.repairshop.auth.infra.client;

import com.cao.repairshop.auth.infra.client.dto.AuthTokenResponseDTO;
import com.cao.repairshop.auth.infra.client.dto.LoginRequestDTO;
import feign.Headers;
import feign.RequestLine;

public interface AuthClient {

    @RequestLine("POST /auth/login")
    @Headers("Content-Type: application/json")
    AuthTokenResponseDTO login(LoginRequestDTO request);
}
