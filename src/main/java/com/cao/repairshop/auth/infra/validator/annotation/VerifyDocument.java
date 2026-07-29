package com.cao.repairshop.auth.infra.validator.annotation;

import com.cao.repairshop.auth.infra.validator.impl.VerifyDocumentValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = VerifyDocumentValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface VerifyDocument {
    String message() default "Documento CPF inválido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
