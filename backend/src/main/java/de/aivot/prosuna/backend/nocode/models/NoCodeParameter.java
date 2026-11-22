package de.aivot.prosuna.backend.nocode.models;


import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
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
        NoCodeParameterOption... options
) {
    public NoCodeParameter {
        // Older serialized plugin signatures do not include the presentation flag.
        optionsAsSentencePart = Boolean.TRUE.equals(optionsAsSentencePart);
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

    @Nonnull
    public NoCodeParameter withSentence(@Nullable String sentencePhrase) {
        return withSentence(sentencePhrase, null);
    }

    @Nonnull
    public NoCodeParameter withSentence(@Nullable String sentencePhrase, @Nullable String sentenceSuffix) {
        return new NoCodeParameter(type, label, description, sentencePhrase, sentenceSuffix, numberConstraints, quantityParameterIndex, optionsAsSentencePart, options);
    }

    @Nonnull
    public NoCodeParameter withNumberConstraints(@Nonnull NoCodeNumberConstraints numberConstraints) {
        return new NoCodeParameter(type, label, description, sentencePhrase, sentenceSuffix, numberConstraints, quantityParameterIndex, optionsAsSentencePart, options);
    }

    /** Marks fixed options as units whose singular label depends on a sibling quantity operand. */
    @Nonnull
    public NoCodeParameter withQuantityParameterIndex(int quantityParameterIndex) {
        return new NoCodeParameter(type, label, description, sentencePhrase, sentenceSuffix, numberConstraints, quantityParameterIndex, optionsAsSentencePart, options);
    }

    /** A dynamic or unknown sentence part requires the editor to show named inputs instead of a sentence. */
    @Nonnull
    public NoCodeParameter withOptionsAsSentencePart() {
        return new NoCodeParameter(type, label, description, sentencePhrase, sentenceSuffix, numberConstraints, quantityParameterIndex, true, options);
    }
}
