package de.aivot.prosuna.backend.elements.models.input;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

/**
 * Persisted representation of every authored element value. Literal values use the same envelope as dynamic values,
 * which keeps the authored/effective boundary explicit throughout the system. The contract is serializable because
 * Hibernate uses Java serialization to create dirty-check snapshots of the converted {@code AuthoredElementValues}.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LiteralAuthoredInputValue.class, name = "Literal"),
        @JsonSubTypes.Type(value = VariableAuthoredInputValue.class, name = "Variable"),
        @JsonSubTypes.Type(value = NoCodeAuthoredInputValue.class, name = "NoCode"),
        @JsonSubTypes.Type(value = LowCodeAuthoredInputValue.class, name = "LowCode")
})
public sealed interface AuthoredInputValue extends Serializable permits LiteralAuthoredInputValue, VariableAuthoredInputValue, NoCodeAuthoredInputValue, LowCodeAuthoredInputValue {
    String type();
}
