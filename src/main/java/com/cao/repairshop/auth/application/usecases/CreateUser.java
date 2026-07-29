package com.cao.repairshop.auth.application.usecases;

import com.cao.repairshop.auth.infra.controller.dtos.AuthTokenResponseDTO;
import com.cao.repairshop.auth.infra.controller.dtos.RegisterRequestDTO;

public interface CreateUser {
    AuthTokenResponseDTO execute(RegisterRequestDTO request);
}
