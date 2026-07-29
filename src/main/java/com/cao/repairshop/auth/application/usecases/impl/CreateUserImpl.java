package com.cao.repairshop.auth.application.usecases.impl;

import com.cao.repairshop.auth.application.gateways.UserGateway;
import com.cao.repairshop.auth.application.usecases.CreateUser;
import com.cao.repairshop.auth.domain.entities.User;
import com.cao.repairshop.auth.infra.controller.dtos.AuthTokenResponseDTO;
import com.cao.repairshop.auth.infra.controller.dtos.RegisterRequestDTO;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class CreateUserImpl implements CreateUser {

    private static final String ISSUER = "https://repairshop.auth.com";
    private static final long DEFAULT_EXPIRES_IN_SECONDS = 86400;

    @Inject
    UserGateway userGateway;

    @Override
    @Transactional
    public AuthTokenResponseDTO execute(RegisterRequestDTO request) {
        if (request == null || request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("E-mail é obrigatório para cadastro");
        }

        String role = (request.function() != null && !request.function().isBlank()) ? request.function() : "CUSTOMER";

        Optional<User> existing = userGateway.findByEmail(request.email());
        if (existing.isEmpty() && request.password() != null && !request.password().isBlank()) {
            User user = User.builder()
                    .name(request.name() != null ? request.name() : "Usuário RepairShop")
                    .email(request.email())
                    .function(role)
                    .phone(request.phone())
                    .password(BcryptUtil.bcryptHash(request.password()))
                    .build();
            userGateway.save(user);
        }

        String token = generateJwtToken(request.email(), Set.of(role));
        return AuthTokenResponseDTO.bearer(token, DEFAULT_EXPIRES_IN_SECONDS);
    }

    private String generateJwtToken(String subject, Set<String> groups) {
        long now = Instant.now().getEpochSecond();
        return Jwt.issuer(ISSUER)
                .subject(subject)
                .groups(groups)
                .claim("email", subject)
                .issuedAt(now)
                .expiresAt(now + DEFAULT_EXPIRES_IN_SECONDS)
                .sign();
    }
}
