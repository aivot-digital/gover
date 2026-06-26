package de.aivot.GoverBackend.nocode.models;

import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.util.List;

public record NoCodeOperandError(
        @Nullable
        NoCodeOperand operand,
        @Nullable
        String error,
        @Nullable
        List<NoCodeOperandError> subErrors
) {
    public boolean isValid() {
        if (StringUtils.isNotNullOrEmpty(error)) {
            return false;
        }

        if (subErrors != null && !subErrors.isEmpty()) {
            return subErrors
                    .stream()
                    .allMatch(NoCodeOperandError::isValid);
        }

        return true;
    }

    public static NoCodeOperandError NO_ERROR(NoCodeOperand operand) {
        return new NoCodeOperandError(operand, null, null);
    }
}
