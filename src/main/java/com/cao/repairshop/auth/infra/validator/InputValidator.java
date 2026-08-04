package com.cao.repairshop.auth.infra.validator;

import com.cao.repairshop.auth.domain.exception.ValidationException;
import com.cao.repairshop.auth.domain.model.Credentials;

import java.util.regex.Pattern;

/**
 * Validador defensivo de segurança de entrada de dados.
 */
public class InputValidator {

    // Regex rigoroso baseado no RFC 5322 para validação de e-mails válidos
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    public static void validateCredentials(Credentials credentials) {
        if (credentials == null) {
            throw new ValidationException("O objeto de credenciais não pode ser nulo.");
        }

        String email = credentials.getEmail();
        if (email == null || email.isBlank()) {
            throw new ValidationException("O campo 'email' é obrigatório.");
        }

        if (email.length() > 255) {
            throw new ValidationException("O campo 'email' não pode exceder 255 caracteres.");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("O formato do e-mail informado é inválido.");
        }

        String password = credentials.getPassword();
        if (password == null || password.isBlank()) {
            throw new ValidationException("O campo 'password' é obrigatório.");
        }

        if (password.length() > 100) {
            throw new ValidationException("O campo 'password' excede o tamanho máximo permitido.");
        }
    }
}
