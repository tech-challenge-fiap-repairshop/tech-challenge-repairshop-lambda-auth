package com.cao.repairshop.auth.infra.validator;

import com.cao.repairshop.auth.domain.exception.ValidationException;
import com.cao.repairshop.auth.domain.model.Credentials;

import java.util.regex.Pattern;

/**
 * Validador defensivo de segurança de entrada de dados.
 */
public class InputValidator {

    public static void validateCredentials(Credentials credentials) {
        if (credentials == null) {
            throw new ValidationException("O objeto de credenciais não pode ser nulo.");
        }

        String cpf = credentials.getCpf();
        if (cpf == null || cpf.isBlank()) {
            throw new ValidationException("O campo 'cpf' é obrigatório.");
        }

        if (!isValidCpf(cpf)) {
            throw new ValidationException("O formato do CPF informado é inválido.");
        }

        String password = credentials.getPassword();
        if (password == null || password.isBlank()) {
            throw new ValidationException("O campo 'password' é obrigatório.");
        }

        if (password.length() > 100) {
            throw new ValidationException("O campo 'password' excede o tamanho máximo permitido.");
        }
    }

    private static boolean isValidCpf(String cpf) {
        if (cpf == null) return false;
        String digitsOnly = cpf.replaceAll("\\D", "");
        if (digitsOnly.length() != 11 || digitsOnly.matches("(\\d)\\1{10}")) {
            return false;
        }
        try {
            int d1 = 0, d2 = 0;
            for (int i = 0; i < 9; i++) {
                int digit = Character.getNumericValue(digitsOnly.charAt(i));
                d1 += digit * (10 - i);
                d2 += digit * (11 - i);
            }
            int r1 = 11 - (d1 % 11);
            if (r1 >= 10) r1 = 0;
            if (r1 != Character.getNumericValue(digitsOnly.charAt(9))) return false;

            d2 += r1 * 2;
            int r2 = 11 - (d2 % 11);
            if (r2 >= 10) r2 = 0;
            return r2 == Character.getNumericValue(digitsOnly.charAt(10));
        } catch (Exception e) {
            return false;
        }
    }
}
