package com.cao.repairshop.auth.infra.controller.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
    @NotBlank(message = "O documento/CPF ou e-mail é obrigatório")
    String cpf,

    @NotBlank(message = "A senha é obrigatória")
    String password
) {}
