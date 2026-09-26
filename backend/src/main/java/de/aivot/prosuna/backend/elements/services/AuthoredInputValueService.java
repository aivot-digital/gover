package de.aivot.prosuna.backend.elements.services;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.models.input.AuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.LiteralAuthoredInputValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Converts ordinary values into authored envelopes using the element tree to identify nested container rows.
 * Dynamic values are resolved by the derivation service, not by these structural conversions.
 */
@Service
public class AuthoredInputValueService {
    private final JsonMapper jsonMapper;

    public AuthoredInputValueService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Nullable
    public Object unwrapLiteral(@Nullable AuthoredInputValue value) {
        return value instanceof LiteralAuthoredInputValue literal ? literal.value() : null;
    }

    // Only wrap the current level here; callers must normalize container children using the element tree.
    @Nonnull
    private AuthoredElementValues wrapLiteralLevel(@Nullable Object value) {
        if (value == null) {
            return new AuthoredElementValues();
        }

        var rawValues = toMap(value);
        var result = new AuthoredElementValues();
        for (var entry : rawValues.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.putLiteral(key, entry.getValue());
            }
        }
        return result;
    }

    /**
     * Converts effective values back into editable literal values. The element tree is required because only it can
     * distinguish a replicating-container row from an arbitrary list of business objects.
     */
    @Nonnull
    public AuthoredElementValues toLiteralAuthoredElementValues(
            @Nonnull BaseElement rootElement,
            @Nullable Object effectiveValues
    ) {
        var rawValues = effectiveValues == null ? Map.of() : toMap(effectiveValues);
        var result = wrapLiteralLevel(rawValues);
        normalizeReplicatingContainers(rootElement, rawValues, result);
        return result;
    }

    private void normalizeReplicatingContainers(
            @Nonnull BaseElement element,
            @Nonnull Map<?, ?> effectiveValues,
            @Nonnull AuthoredElementValues authoredValues
    ) {
        if (element instanceof ReplicatingContainerLayoutElement replicatingContainer) {
            if (effectiveValues.containsKey(element.getId())) {
                authoredValues.putLiteral(
                        element.getId(),
                        toLiteralReplicatingContainerValue(replicatingContainer, effectiveValues.get(element.getId()))
                );
            }
            return;
        }

        if (element instanceof LayoutElement<?> layoutElement) {
            for (var child : layoutElement.getChildren()) {
                normalizeReplicatingContainers(child, effectiveValues, authoredValues);
            }
        }
    }

    @Nullable
    private Object toLiteralReplicatingContainerValue(
            @Nonnull ReplicatingContainerLayoutElement element,
            @Nullable Object effectiveValue
    ) {
        if (!(effectiveValue instanceof Collection<?> rows)) {
            return effectiveValue;
        }

        return toLiteralReplicatingContainerRows(element, rows);
    }

    /**
     * Converts effective container rows into the canonical authored row shape, including nested replicating
     * containers. Callers still decide whether an empty collection is a valid field value.
     */
    @Nonnull
    public List<ReplicatingContainerLayoutElementValue> toLiteralReplicatingContainerRows(
            @Nonnull ReplicatingContainerLayoutElement element,
            @Nonnull Collection<?> rows
    ) {

        var authoredRows = new ArrayList<ReplicatingContainerLayoutElementValue>(rows.size());
        for (var rawRow : rows) {
            if (rawRow == null) {
                authoredRows.add(new ReplicatingContainerLayoutElementValue()
                        .setValues(new AuthoredElementValues()));
                continue;
            }

            var row = toMap(rawRow);
            var rawRowValues = row.containsKey("values") ? row.get("values") : row;
            var rowValues = wrapLiteralLevel(rawRowValues);
            var effectiveRowValues = rawRowValues == null ? Map.of() : toMap(rawRowValues);
            for (var child : element.getChildren()) {
                normalizeReplicatingContainers(child, effectiveRowValues, rowValues);
            }

            authoredRows.add(new ReplicatingContainerLayoutElementValue()
                    .setId(row.get("id") instanceof String id ? id : null)
                    .setValues(rowValues));
        }
        return authoredRows;
    }

    @Nonnull
    private Map<?, ?> toMap(@Nonnull Object value) {
        return value instanceof Map<?, ?> map ? map : jsonMapper.convertValue(value, Map.class);
    }
}
