package com.cao.repairshop.auth.domain.exception;

/**
 * Exceção de domínio lançada quando falha a validação defensiva das credenciais de entrada.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
