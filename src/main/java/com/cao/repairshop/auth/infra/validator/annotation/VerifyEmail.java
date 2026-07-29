package com.cao.repairshop.auth.infra.validator.annotation;

import com.cao.repairshop.auth.infra.validator.impl.VerifyEmailValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = VerifyEmailValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface VerifyEmail {
    String message() default "E-mail inválido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
