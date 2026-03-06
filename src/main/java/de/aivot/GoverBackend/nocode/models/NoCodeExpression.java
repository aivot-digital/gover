package de.aivot.GoverBackend.nocode.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * Represents an expression in the NoCode language.
 * An expression consists of an operator and a list of operands.
 */
public class NoCodeExpression extends NoCodeOperand {
    public static final String TYPE_ID = "NoCodeExpression";

    @Nullable
    private String operatorIdentifier;
    @Nullable
    private List<NoCodeOperand> operands;

    public NoCodeExpression() {
        super(TYPE_ID);
    }

    public NoCodeExpression(@Nonnull String operatorIdentifier, @Nonnull NoCodeOperand... operands) {
        super(TYPE_ID);
        this.operatorIdentifier = operatorIdentifier;
        this.operands = Arrays.stream(operands).toList();
    }

    public static NoCodeExpression of(@Nonnull String operatorIdentifier, @Nonnull NoCodeOperand... operands) {
        return new NoCodeExpression(operatorIdentifier, operands);
    }

    public boolean isEmpty() {
        return StringUtils.isNullOrEmpty(operatorIdentifier) && (operands == null || operands.isEmpty());
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    @JsonIgnore
    public Set<String> getReferencedIds() {
        if (operands == null || operands.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> referencedIds = new HashSet<>();
        for (NoCodeOperand operand : operands) {
            if (operand != null) {
                switch (operand) {
                    case NoCodeReference reference -> {
                        if (reference.getElementId() != null) {
                            referencedIds.add(reference.getElementId());
                        }
                    }
                    case NoCodeExpression expression -> referencedIds.addAll(expression.getReferencedIds());
                    default -> {
                    }
                }
            }
        }

        return referencedIds;
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NoCodeExpression that = (NoCodeExpression) o;
        return Objects.equals(operatorIdentifier, that.operatorIdentifier) && Objects.equals(operands, that.operands);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(operatorIdentifier);
        result = 31 * result + Objects.hashCode(operands);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getOperatorIdentifier() {
        return operatorIdentifier;
    }

    public NoCodeExpression setOperatorIdentifier(@Nullable String operatorIdentifier) {
        this.operatorIdentifier = operatorIdentifier;
        return this;
    }

    public NoCodeExpression setOperands(@Nullable List<NoCodeOperand> operands) {
        this.operands = operands;
        return this;
    }

    /**
     * This collection may contain null values, which represent null operands.
     *
     * @return The operands of the expression.
     */
    @Nullable
    public Collection<NoCodeOperand> getOperands() {
        return operands;
    }
}
