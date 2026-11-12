package de.aivot.prosuna.backend.plugins.core.v1.operators.text;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

/**
 * Literal text comparisons that replace the legacy text condition operators without hand-written regular expressions.
 */
public class NoCodeTextCompareOperator extends NoCodeOperator {
    public enum Mode {
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        EQUALS_IGNORE_CASE,
    }

    private final Mode mode;

    public NoCodeTextCompareOperator(Mode mode) {
        this.mode = mode;
    }

    @Override
    public String getIdentifier() {
        return switch (mode) {
            case CONTAINS -> "text-contains";
            case STARTS_WITH -> "text-starts-with";
            case ENDS_WITH -> "text-ends-with";
            case EQUALS_IGNORE_CASE -> "text-equals-ignore-case";
        };
    }

    @Override
    public String getLabel() {
        return switch (mode) {
            case CONTAINS -> "Text beinhaltet";
            case STARTS_WITH -> "Text beginnt mit";
            case ENDS_WITH -> "Text endet mit";
            case EQUALS_IGNORE_CASE -> "Text ist gleich (ohne Groß-/Kleinschreibung)";
        };
    }

    @Override
    public String getAbstract() {
        return switch (mode) {
            case CONTAINS -> "Prüft, ob ein Text einen anderen Text enthält.";
            case STARTS_WITH -> "Prüft, ob ein Text mit einem anderen Text beginnt.";
            case ENDS_WITH -> "Prüft, ob ein Text mit einem anderen Text endet.";
            case EQUALS_IGNORE_CASE -> "Prüft, ob zwei Texte ohne Beachtung der Groß-/Kleinschreibung gleich sind.";
        };
    }

    @Override
    public String getDescription() {
        var example = switch (mode) {
            case CONTAINS -> """
                    - **Text** „Antrag auf Förderung“, **Suchtext** „Förderung“ → **wahr**
                    - **Text** „Antrag auf Förderung“, **Suchtext** „förderung“ → **falsch**
                    """;
            case STARTS_WITH -> """
                    - **Text** „DE89370400440532013000“, **Suchtext** „DE“ → **wahr**
                    - **Text** „AT611904300234573201“, **Suchtext** „DE“ → **falsch**
                    """;
            case ENDS_WITH -> """
                    - **Text** „bescheid.pdf“, **Suchtext** „.pdf“ → **wahr**
                    - **Text** „bescheid.docx“, **Suchtext** „.pdf“ → **falsch**
                    """;
            case EQUALS_IGNORE_CASE -> """
                    - **Text** „Müller“, **Vergleichstext** „MÜLLER“ → **wahr**
                    - **Text** „Müller“, **Vergleichstext** „Mueller“ → **falsch**
                    """;
        };

        return """
                # Beschreibung:
                Der Operator **„%s“** %s \s
                Der Vergleich erfolgt wörtlich: Sonderzeichen wie Punkte oder Klammern haben keine besondere Bedeutung. \s
                %s
                Ist ein Wert leer, wird er als leerer Text behandelt.

                # Beispiele:
                %s
                Um das Gegenteil zu prüfen, kombinieren Sie den Operator mit **„Nicht“**.
                """.formatted(
                getLabel(),
                getAbstract().replaceFirst("^Prüft", "prüft"),
                mode == Mode.EQUALS_IGNORE_CASE
                        ? ""
                        : "Groß- und Kleinschreibung werden unterschieden.",
                example
        );
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Text",
                                "Der zu prüfende Text."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                mode == Mode.EQUALS_IGNORE_CASE ? "Vergleichstext" : "Suchtext",
                                mode == Mode.EQUALS_IGNORE_CASE
                                        ? "Der Text, mit dem verglichen werden soll."
                                        : "Der Text, nach dem gesucht werden soll."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return switch (mode) {
            case CONTAINS -> "„#0“ beinhaltet „#1“";
            case STARTS_WITH -> "„#0“ beginnt mit „#1“";
            case ENDS_WITH -> "„#0“ endet mit „#1“";
            case EQUALS_IGNORE_CASE -> "„#0“ ist gleich „#1“ (ohne Groß-/Kleinschreibung)";
        };
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var text = castToString(args[0]);
        var other = castToString(args[1]);

        return new NoCodeResult(switch (mode) {
            case CONTAINS -> text.contains(other);
            case STARTS_WITH -> text.startsWith(other);
            case ENDS_WITH -> text.endsWith(other);
            // Matches the legacy EqualsIgnoreCase condition, including umlauts.
            case EQUALS_IGNORE_CASE -> text.equalsIgnoreCase(other);
        });
    }
}
