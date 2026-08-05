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
    @DisplayName("Deve aceitar credenciais válidas com CPF bem formatado")
    void shouldAcceptValidCredentials() {
        Credentials credentials = new Credentials("52998224725", "secret123");
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
    @ValueSource(strings = {"12345678901", "11111111111", "123456", "abcdefghijk"})
    @DisplayName("Deve lançar ValidationException para CPFs mal formatados")
    void shouldThrowValidationExceptionForInvalidCpfFormats(String invalidCpf) {
        Credentials credentials = new Credentials(invalidCpf, "secret123");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> InputValidator.validateCredentials(credentials));
        assertEquals("O formato do CPF informado é inválido.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar ValidationException para senha vazia")
    void shouldThrowValidationExceptionForBlankPassword() {
        Credentials credentials = new Credentials("52998224725", "   ");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> InputValidator.validateCredentials(credentials));
        assertEquals("O campo 'password' é obrigatório.", exception.getMessage());
    }
}
