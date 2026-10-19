package de.aivot.prosuna.backend.elements.models;

import de.aivot.prosuna.backend.elements.models.input.AuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.LiteralAuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.LowCodeAuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.NoCodeAuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.VariableAuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import de.aivot.prosuna.backend.utils.MapUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Values persisted or transported for an authored element structure. Every entry has an explicit input-mode envelope;
 * only {@link EffectiveElementValues} contains the resolved domain values used at runtime.
 */
public class AuthoredElementValues extends HashMap<String, AuthoredInputValue> implements Cloneable {
    public static final String LITERAL_VALUE_PROPERTY = "value";

    /**
     * Maps a domain path to the persisted payload of its top-level literal input-mode envelope.
     */
    public static List<String> literalValueJsonPath(String... domainPath) {
        if (domainPath.length == 0) {
            throw new IllegalArgumentException("A literal authored value JSON path requires an element id.");
        }

        var jsonPath = new ArrayList<String>(domainPath.length + 1);
        jsonPath.add(domainPath[0]);
        jsonPath.add(LITERAL_VALUE_PROPERTY);
        jsonPath.addAll(List.of(domainPath).subList(1, domainPath.length));
        return List.copyOf(jsonPath);
    }

    public AuthoredElementValues putLiteral(String key, Object value) {
        put(key, new LiteralAuthoredInputValue(value));
        return this;
    }

    @Override
    public AuthoredInputValue put(String key, AuthoredInputValue value) {
        return super.put(
                Objects.requireNonNull(key, "An authored element value requires an element id."),
                Objects.requireNonNull(value, "An authored element value must use an input-mode envelope.")
        );
    }

    @Override
    public void putAll(Map<? extends String, ? extends AuthoredInputValue> values) {
        values.forEach(this::put);
    }

    public Object getLiteral(String key) {
        var value = get(key);
        return value instanceof LiteralAuthoredInputValue literal ? literal.value() : null;
    }

    @Override
    @Nonnull
    public AuthoredElementValues clone() {
        var clone = (AuthoredElementValues) super.clone();
        for (var entry : entrySet()) {
            clone.put(entry.getKey(), copyInputValue(entry.getValue()));
        }
        return clone;
    }

    @Nonnull
    private static AuthoredInputValue copyInputValue(@Nonnull AuthoredInputValue value) {
        return switch (value) {
            case LiteralAuthoredInputValue literal -> new LiteralAuthoredInputValue(copyValue(literal.value()));
            case NoCodeAuthoredInputValue noCode -> new NoCodeAuthoredInputValue(noCode.operand().copy(AuthoredElementValues::copyValue));
            // These records contain only immutable strings, enums and references composed of those types.
            case VariableAuthoredInputValue variable -> variable;
            case LowCodeAuthoredInputValue lowCode -> lowCode;
        };
    }

    @Nullable
    private static Object copyValue(@Nullable Object value) {
        // Handle typed containers before MapUtils turns maps into generic maps. The hook is applied
        // at every depth, including operands and authored rows nested in literal collections.
        return MapUtils.deepCopyValue(value, item -> switch (item) {
            case AuthoredElementValues values -> values.clone();
            case AuthoredInputValue input -> copyInputValue(input);
            case NoCodeOperand operand -> operand.copy(AuthoredElementValues::copyValue);
            case ReplicatingContainerLayoutElementValue row -> new ReplicatingContainerLayoutElementValue()
                    .setId(row.getId())
                    .setValues(row.getValues() == null ? null : row.getValues().clone());
            default -> item;
        });
    }
}
