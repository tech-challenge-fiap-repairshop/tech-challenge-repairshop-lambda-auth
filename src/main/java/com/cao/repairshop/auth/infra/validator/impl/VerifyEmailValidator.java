package com.cao.repairshop.auth.infra.validator.impl;

import com.cao.repairshop.auth.infra.validator.annotation.VerifyEmail;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class VerifyEmailValidator implements ConstraintValidator<VerifyEmail, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return EMAIL_PATTERN.matcher(value).matches();
    }
}
