package com.emakers.library_api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidCPFValidator implements ConstraintValidator<ValidCPF, String> {

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        if (cpf == null || cpf.isEmpty()) {
            return false;
        }

        cpf = cpf.replaceAll("[^0-9]", "");

        if (cpf.length() != 11) {
            return false;
        }

        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        int sum = 0;
        int remainder;

        for (int i = 1; i <= 9; i++) {
            sum += Character.getNumericValue(cpf.charAt(i - 1)) * (11 - i);
        }
        remainder = (sum * 10) % 11;
        if (remainder == 10 || remainder == 11) {
            remainder = 0;
        }
        if (remainder != Character.getNumericValue(cpf.charAt(9))) {
            return false;
        }

        sum = 0;
        for (int i = 1; i <= 10; i++) {
            sum += Character.getNumericValue(cpf.charAt(i - 1)) * (12 - i);
        }
        remainder = (sum * 10) % 11;
        if (remainder == 10 || remainder == 11) {
            remainder = 0;
        }

        return remainder == Character.getNumericValue(cpf.charAt(10));
    }
}
