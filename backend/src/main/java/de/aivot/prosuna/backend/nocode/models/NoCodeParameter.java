package de.aivot.prosuna.backend.nocode.models;


import com.fasterxml.jackson.annotation.JsonInclude;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDateInput;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A parameter of an operator signature.
 * <p>
 * The sentence parts let the editor read an expression as a sentence, such as „ergibt wahr, wenn A gleich B“.
 * The phrase precedes the operand and the suffix follows it. A {@code null} phrase falls back to the label, while an
 * empty phrase shows no text before the operand.
 *
 * @param sentencePhrase text before the operand, or {@code null} to show the label
 * @param sentenceSuffix text after the operand, or {@code null} for none
 * @param numberConstraints optional validation rules for authored numeric values
 * @param quantityParameterIndex sibling quantity for unit option labels, or {@code null} for ordinary options
 * @param dateInput supported calendar input forms, or {@code null} for the default editor
 * @param preserveWhitespace whether authored text keeps its leading and trailing whitespace
 * @param optionsAsSentencePart whether a selected fixed option supplies a sentence part whose meaning must be known
 */
public record NoCodeParameter(
        NoCodeDataType type,
        String label,
        String description,
        @Nullable String sentencePhrase,
        @Nullable String sentenceSuffix,
        @Nullable NoCodeNumberConstraints numberConstraints,
        @Nullable Integer quantityParameterIndex,
        @Nullable Boolean optionsAsSentencePart,
        @Nullable NoCodeDateInput dateInput,
        @JsonInclude(JsonInclude.Include.NON_DEFAULT) @Nullable Boolean preserveWhitespace,
        NoCodeParameterOption... options
) {
    public NoCodeParameter {
        // Older serialized plugin signatures do not include these flags.
        optionsAsSentencePart = Boolean.TRUE.equals(optionsAsSentencePart);
        preserveWhitespace = Boolean.TRUE.equals(preserveWhitespace);
    }

    /**
     * Creates a parameter without sentence parts. Kept for operators of plugins that predate the sentence parts.
     */
    public NoCodeParameter(
            NoCodeDataType type,
            String label,
            String description,
            NoCodeParameterOption... options
    ) {
        this(type, label, description, null, null, null, null, false, options);
    }

    /** Preserves the constructor used by plugins that already supply sentence parts. */
    public NoCodeParameter(
            NoCodeDataType type,
            String label,
            String description,
            @Nullable String sentencePhrase,
            @Nullable String sentenceSuffix,
            NoCodeParameterOption... options
    ) {
        this(type, label, description, sentencePhrase, sentenceSuffix, null, null, false, options);
    }

    /** Preserves the constructor used by plugins that supply number or quantity metadata. */
    public NoCodeParameter(
            NoCodeDataType type,
            String label,
            String description,
            @Nullable String sentencePhrase,
            @Nullable String sentenceSuffix,
            @Nullable NoCodeNumberConstraints numberConstraints,
            @Nullable Integer quantityParameterIndex,
            NoCodeParameterOption... options
    ) {
        this(type, label, description, sentencePhrase, sentenceSuffix, numberConstraints, quantityParameterIndex, false, options);
    }

    /** Preserves the constructor used by plugins that supply sentence-part options. */
    public NoCodeParameter(
            NoCodeDataType type,
            String label,
            String description,
            @Nullable String sentencePhrase,
            @Nullable String sentenceSuffix,
            @Nullable NoCodeNumberConstraints numberConstraints,
            @Nullable Integer quantityParameterIndex,
            @Nullable Boolean optionsAsSentencePart,
            NoCodeParameterOption... options
    ) {
        this(type, label, description, sentencePhrase, sentenceSuffix, numberConstraints, quantityParameterIndex, optionsAsSentencePart, null, options);
    }

    /** Preserves the constructor used by plugins that supply calendar input metadata. */
    public NoCodeParameter(
            NoCodeDataType type,
            String label,
            String description,
            @Nullable String sentencePhrase,
            @Nullable String sentenceSuffix,
            @Nullable NoCodeNumberConstraints numberConstraints,
            @Nullable Integer quantityParameterIndex,
            @Nullable Boolean optionsAsSentencePart,
            @Nullable NoCodeDateInput dateInput,
            NoCodeParameterOption... options
    ) {
        this(type, label, description, sentencePhrase, sentenceSuffix, numberConstraints, quantityParameterIndex, optionsAsSentencePart, dateInput, false, options);
    }

    @Nonnull
    public NoCodeParameter withSentence(@Nullable String sentencePhrase) {
        return withSentence(sentencePhrase, null);
    }

    @Nonnull
    public NoCodeParameter withSentence(@Nullable String sentencePhrase, @Nullable String sentenceSuffix) {
        return new NoCodeParameter(type, label, description, sentencePhrase, sentenceSuffix, numberConstraints, quantityParameterIndex, optionsAsSentencePart, dateInput, preserveWhitespace, options);
    }

    @Nonnull
    public NoCodeParameter withNumberConstraints(@Nonnull NoCodeNumberConstraints numberConstraints) {
        return new NoCodeParameter(type, label, description, sentencePhrase, sentenceSuffix, numberConstraints, quantityParameterIndex, optionsAsSentencePart, dateInput, preserveWhitespace, options);
    }

    /** Marks fixed options as units whose singular label depends on a sibling quantity operand. */
    @Nonnull
    public NoCodeParameter withQuantityParameterIndex(int quantityParameterIndex) {
        return new NoCodeParameter(type, label, description, sentencePhrase, sentenceSuffix, numberConstraints, quantityParameterIndex, optionsAsSentencePart, dateInput, preserveWhitespace, options);
    }

    /** A dynamic or unknown sentence part requires the editor to show named inputs instead of a sentence. */
    @Nonnull
    public NoCodeParameter withOptionsAsSentencePart() {
        return new NoCodeParameter(type, label, description, sentencePhrase, sentenceSuffix, numberConstraints, quantityParameterIndex, true, dateInput, preserveWhitespace, options);
    }

    @Nonnull
    public NoCodeParameter withDateInput(@Nullable NoCodeDateInput dateInput) {
        return new NoCodeParameter(type, label, description, sentencePhrase, sentenceSuffix, numberConstraints, quantityParameterIndex, optionsAsSentencePart, dateInput, preserveWhitespace, options);
    }

    @Nonnull
    public NoCodeParameter withPreservedWhitespace() {
        return new NoCodeParameter(type, label, description, sentencePhrase, sentenceSuffix, numberConstraints, quantityParameterIndex, optionsAsSentencePart, dateInput, true, options);
    }
}
