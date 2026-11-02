package com.example.nexuscore.annotations;

import com.example.nexuscore.util.OptionalNotBlankValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = OptionalNotBlankValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalNotBlank {
    String message() default "não deve estar em branco";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
