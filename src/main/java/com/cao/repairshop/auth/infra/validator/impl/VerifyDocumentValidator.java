package com.cao.repairshop.auth.infra.validator.impl;

import com.cao.repairshop.auth.infra.validator.annotation.VerifyDocument;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class VerifyDocumentValidator implements ConstraintValidator<VerifyDocument, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String unformatted = value.replaceAll("\\D", "");
        if (unformatted.length() != 11) {
            return false;
        }

        if (unformatted.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            int sum1 = 0;
            for (int i = 0; i < 9; i++) {
                sum1 += Character.getNumericValue(unformatted.charAt(i)) * (10 - i);
            }
            int digit1 = 11 - (sum1 % 11);
            if (digit1 >= 10) digit1 = 0;

            if (digit1 != Character.getNumericValue(unformatted.charAt(9))) {
                return false;
            }

            int sum2 = 0;
            for (int i = 0; i < 10; i++) {
                sum2 += Character.getNumericValue(unformatted.charAt(i)) * (11 - i);
            }
            int digit2 = 11 - (sum2 % 11);
            if (digit2 >= 10) digit2 = 0;

            return digit2 == Character.getNumericValue(unformatted.charAt(10));
        } catch (Exception e) {
            return false;
        }
    }
}
