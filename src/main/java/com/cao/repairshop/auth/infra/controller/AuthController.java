package com.cao.repairshop.auth.infra.controller;

import com.cao.repairshop.auth.application.usecases.AuthenticateUser;
import com.cao.repairshop.auth.application.usecases.CreateUser;
import com.cao.repairshop.auth.infra.controller.dtos.AuthTokenResponseDTO;
import com.cao.repairshop.auth.infra.controller.dtos.LoginRequestDTO;
import com.cao.repairshop.auth.infra.controller.dtos.RegisterRequestDTO;
import com.cao.repairshop.auth.infra.controller.interfaces.AuthApi;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/auth")
public class AuthController implements AuthApi {

    @Inject
    AuthenticateUser authenticateUser;

    @Inject
    CreateUser createUser;

    @Override
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "service", "tech-challenge-repairshop-lambda-auth",
            "runtime", "Quarkus - Java " + System.getProperty("java.version")
        )).build();
    }

    @Override
    public Response login(@Valid LoginRequestDTO request) {
        try {
            AuthTokenResponseDTO token = authenticateUser.execute(request);
            return Response.ok(token).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @Override
    public Response register(@Valid RegisterRequestDTO request) {
        try {
            AuthTokenResponseDTO token = createUser.execute(request);
            return Response.status(Response.Status.CREATED)
                    .entity(token)
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
