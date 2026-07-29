package com.cao.repairshop.auth.infra.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class AuthControllerTest {

    @Test
    public void testHealthEndpoint() {
        given()
          .when().get("/auth/health")
          .then()
             .statusCode(200)
             .body("status", is("UP"));
    }

    @Test
    public void testLoginEndpoint() {
        String payload = """
            {
                "cpf": "carlos@repairshop.com",
                "password": "secretpassword"
            }
            """;

        given()
          .contentType(ContentType.JSON)
          .body(payload)
          .when().post("/auth/login")
          .then()
             .statusCode(200)
             .body("token", notNullValue())
             .body("tokenType", is("Bearer"));
    }

    @Test
    public void testRegisterEndpoint() {
        String payload = """
            {
                "name": "Carlos Mechanic",
                "function": "ATTENDANT",
                "email": "carlos@repairshop.com",
                "phone": "+55 11 99999-9999",
                "password": "securePass123"
            }
            """;

        given()
          .contentType(ContentType.JSON)
          .body(payload)
          .when().post("/auth/register")
          .then()
             .statusCode(201)
             .body("token", notNullValue())
             .body("tokenType", is("Bearer"));
    }
}
