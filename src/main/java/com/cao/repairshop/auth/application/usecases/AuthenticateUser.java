package com.cao.repairshop.auth.application.usecases;

import com.cao.repairshop.auth.infra.controller.dtos.AuthTokenResponseDTO;
import com.cao.repairshop.auth.infra.controller.dtos.LoginRequestDTO;

public interface AuthenticateUser {
    AuthTokenResponseDTO execute(LoginRequestDTO request);
}
