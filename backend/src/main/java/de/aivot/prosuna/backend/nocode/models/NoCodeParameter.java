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
 */
public record NoCodeParameter(
        NoCodeDataType type,
        String label,
        String description,
        @Nullable String sentencePhrase,
        @Nullable String sentenceSuffix,
        NoCodeParameterOption... options
) {
    /**
     * Creates a parameter without sentence parts. Kept for operators of plugins that predate the sentence parts.
     */
    public NoCodeParameter(
            NoCodeDataType type,
            String label,
            String description,
            NoCodeParameterOption... options
    ) {
        this(type, label, description, null, null, options);
    }

    @Nonnull
    public NoCodeParameter withSentence(@Nullable String sentencePhrase) {
        return withSentence(sentencePhrase, null);
    }

    @Nonnull
    public NoCodeParameter withSentence(@Nullable String sentencePhrase, @Nullable String sentenceSuffix) {
        return new NoCodeParameter(type, label, description, sentencePhrase, sentenceSuffix, options);
    }
}
