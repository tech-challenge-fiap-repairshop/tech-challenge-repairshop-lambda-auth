package com.cao.repairshop.auth.infra.validator;

import com.cao.repairshop.auth.domain.exception.ValidationException;
import com.cao.repairshop.auth.domain.model.Credentials;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {

    @Test
    @DisplayName("Deve aceitar credenciais válidas com e-mail bem formatado")
    void shouldAcceptValidCredentials() {
        Credentials credentials = new Credentials("carlos@repairshop.com", "secret123");
        assertDoesNotThrow(() -> InputValidator.validateCredentials(credentials));
    }

    @Test
    @DisplayName("Deve lançar ValidationException para credenciais nulas")
    void shouldThrowValidationExceptionForNullCredentials() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> InputValidator.validateCredentials(null));
        assertEquals("O objeto de credenciais não pode ser nulo.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "user@", "@domain.com", "user@domain", "user@.com"})
    @DisplayName("Deve lançar ValidationException para e-mails mal formatados")
    void shouldThrowValidationExceptionForInvalidEmailFormats(String invalidEmail) {
        Credentials credentials = new Credentials(invalidEmail, "secret123");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> InputValidator.validateCredentials(credentials));
        assertEquals("O formato do e-mail informado é inválido.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar ValidationException para e-mail excedendo 255 caracteres")
    void shouldThrowValidationExceptionForTooLongEmail() {
        String longEmail = "a".repeat(245) + "@domain.com";
        Credentials credentials = new Credentials(longEmail, "secret123");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> InputValidator.validateCredentials(credentials));
        assertEquals("O campo 'email' não pode exceder 255 caracteres.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar ValidationException para senha vazia")
    void shouldThrowValidationExceptionForBlankPassword() {
        Credentials credentials = new Credentials("carlos@repairshop.com", "   ");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> InputValidator.validateCredentials(credentials));
        assertEquals("O campo 'password' é obrigatório.", exception.getMessage());
    }
}
