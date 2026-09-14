package com.example.nexuscore.util;

import com.example.nexuscore.annotations.TelephoneList;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

public class TelephoneListValidator implements ConstraintValidator<TelephoneList, List<String>> {

    private static final String REGEX = "^\\+[1-9][0-9]{7,14}$";

    @Override
    public boolean isValid(List<String> telephones, ConstraintValidatorContext context) {

        if (telephones == null) {
            return true;
        }

        for (String number : telephones) {
            if (number == null || !number.matches(REGEX)) {

                context.disableDefaultConstraintViolation();

                context.buildConstraintViolationWithTemplate(
                        "Telefone inválido: " + number
                ).addConstraintViolation();

                return false;
            }
        }

        return true;
    }
}
