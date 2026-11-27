package de.aivot.prosuna.backend.nocode.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

/**
 * Versioned input drafts retain explicit missing/null/empty values and unfinished JSON without coercion.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NoCodeTestData(
        int version,
        @Nonnull Map<String, Value> elementValues,
        @Nonnull Map<Source, SourceValues> sourceValues,
        @Nonnull Map<String, String> visibility
) implements java.io.Serializable {
    public NoCodeTestData {
        if (version != 1) throw new IllegalArgumentException("Die Version der No-Code-Testdaten wird nicht unterstützt.");
        elementValues = elementValues == null ? Map.of() : Map.copyOf(elementValues);
        sourceValues = sourceValues == null ? Map.of() : Map.copyOf(sourceValues);
        visibility = visibility == null ? Map.of() : Map.copyOf(visibility);
    }

    public enum Source {processData, instanceData, nodeData}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(@Nonnull String text, @Nonnull NoCodeDataType type, @Nonnull String mode) implements java.io.Serializable {
        public Value {
            if (text == null || type == null || !java.util.Set.of("value", "json", "missing", "null", "empty").contains(mode == null ? "" : mode)) {
                throw new IllegalArgumentException("Die No-Code-Testeingabe ist ungültig.");
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SourceValues(@Nullable String mode, @Nullable String json, @Nonnull Map<String, Value> values) implements java.io.Serializable {
        public SourceValues {
            if (mode != null && !mode.equals("fields") && !mode.equals("json")) {
                throw new IllegalArgumentException("Das Eingabeformat der No-Code-Testdaten ist ungültig.");
            }
            values = values == null ? Map.of() : Map.copyOf(values);
        }
    }
}
