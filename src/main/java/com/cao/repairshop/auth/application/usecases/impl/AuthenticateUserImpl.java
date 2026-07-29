package com.cao.repairshop.auth.application.usecases.impl;

import com.cao.repairshop.auth.application.gateways.UserGateway;
import com.cao.repairshop.auth.application.usecases.AuthenticateUser;
import com.cao.repairshop.auth.domain.entities.User;
import com.cao.repairshop.auth.infra.controller.dtos.AuthTokenResponseDTO;
import com.cao.repairshop.auth.infra.controller.dtos.LoginRequestDTO;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class AuthenticateUserImpl implements AuthenticateUser {

    private static final String ISSUER = "https://repairshop.auth.com";
    private static final long DEFAULT_EXPIRES_IN_SECONDS = 86400;

    @Inject
    UserGateway userGateway;

    @Override
    @Transactional
    public AuthTokenResponseDTO execute(LoginRequestDTO request) {
        if (request == null || (request.cpf() == null && request.password() == null)) {
            throw new IllegalArgumentException("Dados de login inválidos");
        }

        String subject = request.cpf() != null ? request.cpf() : "user";
        String role = "USER";

        if (request.cpf() != null && request.cpf().contains("@")) {
            Optional<User> userOpt = userGateway.findByEmail(request.cpf());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (!BcryptUtil.matches(request.password(), user.getPassword())) {
                    throw new IllegalArgumentException("Credenciais inválidas");
                }
                role = user.getFunction() != null ? user.getFunction() : "USER";
            }
        }

        String token = generateJwtToken(subject, Set.of(role));
        return AuthTokenResponseDTO.bearer(token, DEFAULT_EXPIRES_IN_SECONDS);
    }

    private String generateJwtToken(String subject, Set<String> groups) {
        long now = Instant.now().getEpochSecond();
        return Jwt.issuer(ISSUER)
                .subject(subject)
                .groups(groups)
                .claim("cpf", subject)
                .issuedAt(now)
                .expiresAt(now + DEFAULT_EXPIRES_IN_SECONDS)
                .sign();
    }
}
