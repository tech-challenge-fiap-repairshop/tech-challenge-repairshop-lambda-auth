package com.cao.repairshop.auth.infra.controller.dtos;

import com.cao.repairshop.auth.infra.validator.annotation.VerifyEmail;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequestDTO(
    @NotBlank(message = "O nome é obrigatório")
    String name,

    String function,

    @NotBlank(message = "O e-mail é obrigatório")
    @VerifyEmail(message = "E-mail inválido")
    String email,

    String phone,

    @NotBlank(message = "A senha é obrigatória")
    String password
) {}
