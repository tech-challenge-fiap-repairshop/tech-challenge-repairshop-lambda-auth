package com.cao.repairshop.auth.infra.config;

import com.cao.repairshop.auth.infra.client.AuthClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;

import java.util.concurrent.TimeUnit;

/**
 * Fábrica de configuração resiliênte para instanciar o OpenFeign AuthClient.
 */
public class FeignConfig {

    public static AuthClient createAuthClient(String baseUrl, ObjectMapper objectMapper) {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        // Configuração defensiva de timeouts (3s para conexão, 5s para leitura)
        Request.Options options = new Request.Options(
                3000, TimeUnit.MILLISECONDS,
                5000, TimeUnit.MILLISECONDS,
                true
        );

        return Feign.builder()
                .options(options)
                .retryer(Retryer.NEVER_RETRY)
                .encoder(new JacksonEncoder(objectMapper))
                .decoder(new JacksonDecoder(objectMapper))
                .logger(new Slf4jLogger(AuthClient.class))
                .logLevel(Logger.Level.BASIC)
                .target(AuthClient.class, baseUrl);
    }
}
