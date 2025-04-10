package de.aivot.GoverBackend.models.lib;

import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;
import java.util.Objects;

public class ValidationExpressionWrapper {
    private final NoCodeExpression expression;
    private final String message;

    public ValidationExpressionWrapper(Map<String, Object> values) {
        this.expression = MapUtils.getApply(values, "expression", Map.class, NoCodeExpression::new);
        this.message = MapUtils.getString(values, "message", "");
    }

    public ValidationExpressionWrapper(NoCodeExpression expression, String message) {
        this.expression = expression;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public NoCodeExpression getExpression() {
        return expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
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
}
