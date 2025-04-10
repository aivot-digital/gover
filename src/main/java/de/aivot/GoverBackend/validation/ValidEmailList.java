package de.aivot.GoverBackend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = EmailListValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmailList {
    String message() default "DepartmentMail must be a comma-separated list of valid email addresses";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
