package de.aivot.prosuna.backend.nocode.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Represents an operand in the NoCode language.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoCodeExpression.class, name = NoCodeExpression.TYPE_ID),
        @JsonSubTypes.Type(value = NoCodeReference.class, name = NoCodeReference.TYPE_ID),
        @JsonSubTypes.Type(value = NoCodeProcessDataReference.class, name = NoCodeProcessDataReference.TYPE_ID),
        @JsonSubTypes.Type(value = NoCodeInstanceDataReference.class, name = NoCodeInstanceDataReference.TYPE_ID),
        @JsonSubTypes.Type(value = NoCodeNodeDataReference.class, name = NoCodeNodeDataReference.TYPE_ID),
        @JsonSubTypes.Type(value = NoCodeStaticValue.class, name = NoCodeStaticValue.TYPE_ID),
})
public abstract class NoCodeOperand implements Serializable, Cloneable {
    @Nonnull
    private String type;

    public NoCodeOperand(@Nonnull String type) {
        this.type = type;
    }

    /**
     * Copies this operand without normalizing its stored fields through getters or JSON.
     * Subclasses with mutable payloads must copy them with the supplied recursive value copier.
     */
    @Nonnull
    public NoCodeOperand copy(@Nonnull UnaryOperator<Object> copyValue) {
        try {
            return (NoCodeOperand) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError(exception);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        NoCodeOperand that = (NoCodeOperand) o;
        return Objects.equals(type, that.type);
    }

    @Nonnull
    public abstract NoCodeOperandError validate();

    @Override
    public int hashCode() {
        return Objects.hashCode(type);
    }

    @Nonnull
    public String getType() {
        return type;
    }

    public NoCodeOperand setType(@Nonnull String type) {
        this.type = type;
        return this;
    }
}
