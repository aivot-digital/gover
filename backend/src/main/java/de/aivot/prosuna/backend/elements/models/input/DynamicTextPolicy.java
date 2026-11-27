package de.aivot.prosuna.backend.elements.models.input;

import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.process.enums.TemplateOutputFormat;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Trusted backend policy enabling template syntax inside a literal text value.
 *
 * <p>The source list controls picker suggestions only. Dynamic text accepts expressions and conditions, so it is not
 * a source-level execution sandbox. Authorization must therefore happen before process data reaches derivation.</p>
 *
 * <p>The output format describes how the field's text is used afterwards. It decides how values are inserted, also
 * when the whole field value comes from process data or code, so it must match the consumer of the field. Without one,
 * the field is treated as a single line of text, such as a file name, a mail address or a URL.</p>
 */
public record DynamicTextPolicy(
        @Nonnull List<InputVariableSource> variableSuggestionSources,
        @Nonnull TemplateOutputFormat outputFormat
) implements Serializable {
    public DynamicTextPolicy() {
        this(List.of(InputVariableSource.values()), TemplateOutputFormat.SingleLineText);
    }

    public DynamicTextPolicy(@Nullable List<InputVariableSource> variableSuggestionSources) {
        this(variableSuggestionSources, TemplateOutputFormat.SingleLineText);
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
        if (outputFormat == null) {
            outputFormat = TemplateOutputFormat.SingleLineText;
        }
    }
}
