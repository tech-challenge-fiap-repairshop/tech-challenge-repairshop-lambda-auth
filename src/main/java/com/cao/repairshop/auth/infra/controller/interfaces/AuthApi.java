package com.cao.repairshop.auth.infra.controller.interfaces;

import com.cao.repairshop.auth.infra.controller.dtos.AuthTokenResponseDTO;
import com.cao.repairshop.auth.infra.controller.dtos.LoginRequestDTO;
import com.cao.repairshop.auth.infra.controller.dtos.RegisterRequestDTO;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AuthApi {

    @GET
    @Path("/health")
    Response health();

    @POST
    @Path("/login")
    Response login(@Valid LoginRequestDTO request);

    @POST
    @Path("/register")
    Response register(@Valid RegisterRequestDTO request);
}
