package de.aivot.prosuna.backend.elements.models.input;

import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Trusted backend policy enabling template syntax inside a literal text value.
 *
 * <p>The source list controls picker suggestions only. Dynamic text accepts expressions and conditions, so it is not
 * a source-level execution sandbox. Authorization must therefore happen before process data reaches derivation.</p>
 */
public record DynamicTextPolicy(
        @Nonnull List<InputVariableSource> variableSuggestionSources
) implements Serializable {
    public DynamicTextPolicy() {
        this(List.of(InputVariableSource.values()));
    }

    public DynamicTextPolicy {
        if (variableSuggestionSources == null) {
            variableSuggestionSources = List.of(InputVariableSource.values());
        }
        variableSuggestionSources = List.copyOf(variableSuggestionSources);

        if (variableSuggestionSources.isEmpty()) {
            throw new IllegalArgumentException("At least one variable suggestion source is required for dynamic text.");
        }
        if (new LinkedHashSet<>(variableSuggestionSources).size() != variableSuggestionSources.size()) {
            throw new IllegalArgumentException("Dynamic text variable suggestion sources must not contain duplicates.");
        }
    }
}
