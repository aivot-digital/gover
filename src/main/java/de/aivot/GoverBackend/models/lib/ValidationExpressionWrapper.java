package de.aivot.GoverBackend.models.lib;

import de.aivot.GoverBackend.nocode.models.NoCodeExpression;

import javax.annotation.Nullable;
import java.util.Objects;

public class ValidationExpressionWrapper {
    @Nullable
    private NoCodeExpression expression;
    @Nullable
    private String message;

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ValidationExpressionWrapper that = (ValidationExpressionWrapper) o;
        return Objects.equals(expression, that.expression) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(expression);
        result = 31 * result + Objects.hashCode(message);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public NoCodeExpression getExpression() {
        return expression;
    }

    public ValidationExpressionWrapper setExpression(@Nullable NoCodeExpression expression) {
        this.expression = expression;
        return this;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public ValidationExpressionWrapper setMessage(@Nullable String message) {
        this.message = message;
        return this;
    }

    // endregion
}
