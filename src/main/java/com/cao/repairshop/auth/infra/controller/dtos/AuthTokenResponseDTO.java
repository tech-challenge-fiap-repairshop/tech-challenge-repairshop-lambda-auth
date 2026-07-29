package com.cao.repairshop.auth.infra.controller.dtos;

public record AuthTokenResponseDTO(
    String token,
    String tokenType,
    long expiresIn
) {
    public static AuthTokenResponseDTO bearer(String token, long expiresIn) {
        return new AuthTokenResponseDTO(token, "Bearer", expiresIn);
    }
}
