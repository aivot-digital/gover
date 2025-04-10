package de.aivot.GoverBackend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class EmailListValidator implements ConstraintValidator<ValidEmailList, String> {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Null oder leer ist erlaubt
        }

        return Stream.of(value.split(","))
                .map(String::trim)
                .allMatch(email -> EMAIL_PATTERN.matcher(email).matches());
    }
}
