package de.aivot.prosuna.backend.plugins.core.v1.operators.text;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nonnull;
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
            case CONTAINS -> "Text enthält";
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
        var source = switch (mode) {
            case CONTAINS -> "Betreff";
            case STARTS_WITH -> "Kennung";
            case ENDS_WITH -> "Dateiname";
            case EQUALS_IGNORE_CASE -> "Nachname";
        };
        var exampleText = switch (mode) {
            case CONTAINS -> "Antrag auf Förderung";
            case STARTS_WITH -> "DE89370400440532013000";
            case ENDS_WITH -> "bescheid.pdf";
            case EQUALS_IGNORE_CASE -> "Müller";
        };
        var otherText = switch (mode) {
            case CONTAINS -> "Förderung";
            case STARTS_WITH -> "DE";
            case ENDS_WITH -> ".pdf";
            case EQUALS_IGNORE_CASE -> "MÜLLER";
        };
        var comparisonLabel = mode == Mode.EQUALS_IGNORE_CASE ? "Vergleichstext" : "Suchtext";
        var reading = getHumanReadableTemplate().replace("#0", "„" + source + "“").replace("#1", "„" + otherText + "“");
        var purpose = switch (mode) {
            case CONTAINS -> "Prüfen Sie, ob ein Text einen bestimmten Suchtext enthält, beispielsweise ein Stichwort im Betreff.";
            case STARTS_WITH -> "Prüfen Sie, ob ein Text mit einem bestimmten Suchtext beginnt, beispielsweise eine Kennung mit einem Länderpräfix.";
            case ENDS_WITH -> "Prüfen Sie, ob ein Text mit einem bestimmten Suchtext endet, beispielsweise ein Dateiname mit einer Dateiendung.";
            case EQUALS_IGNORE_CASE -> "Prüfen Sie, ob zwei Texte übereinstimmen, auch wenn sie unterschiedlich groß- oder kleingeschrieben sind.";
        };
        return """
                # Wofür verwenden Sie diese Funktion?
                %s

                # Welche Werte werden benötigt?
                **Text:** Der zu prüfende Text. **%s:** Der Text, mit dem verglichen wird.

                Beide Texte können Sie frei eingeben oder aus einer anderen Wertquelle beziehungsweise Funktion übernehmen. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                **Wahr**, wenn die beschriebene Bedingung zutrifft; andernfalls **Falsch**.

                # Beispiel im No-Code-Editor
                Wählen Sie in einer Bedingung **„%s“**. Verwenden Sie für **Text** die Variable „%s“, die `%s` liefert, und geben Sie als **%s** `%s` ein.

                Zugeklappt und in der Vorschau lesen Sie: `%s`. Das Ergebnis ist **Wahr**. Um die umgekehrte Bedingung zu prüfen, setzen Sie diesen Ausdruck in **„Nicht“** ein.

                # Was ist zu beachten?
                Der Vergleich erfolgt wörtlich. Punkte, Klammern und andere Sonderzeichen haben keine besondere Bedeutung. Leerzeichen am Anfang und Ende beider Texte zählen beim Vergleich mit. %s

                Fehlende Werte werden als leerer Text behandelt. %s
                """.formatted(
                purpose, comparisonLabel, getLabel(), source, exampleText, comparisonLabel, otherText, reading,
                mode == Mode.EQUALS_IGNORE_CASE
                        ? "Groß- und Kleinschreibung werden ignoriert; beispielsweise passen „Müller“ und „MÜLLER“ zusammen, „Müller“ und „Mueller“ dagegen nicht."
                        : "Groß- und Kleinschreibung werden unterschieden.",
                mode == Mode.EQUALS_IGNORE_CASE
                        ? "Zwei leere Texte ergeben Wahr."
                        : "Ein leerer Suchtext ergibt Wahr, auch bei einem leeren Text."
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
                        ).withSentence("wenn der Text").withPreservedWhitespace(),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                mode == Mode.EQUALS_IGNORE_CASE ? "Vergleichstext" : "Suchtext",
                                mode == Mode.EQUALS_IGNORE_CASE
                                        ? "Der Text, mit dem verglichen werden soll."
                                        : "Der Text, nach dem gesucht werden soll."
                        ).withSentence(switch (mode) {
                            case CONTAINS -> "den Text";
                            case STARTS_WITH, ENDS_WITH -> "mit dem Text";
                            case EQUALS_IGNORE_CASE -> "dem Text";
                        }, switch (mode) {
                            case CONTAINS -> "enthält";
                            case STARTS_WITH -> "beginnt";
                            case ENDS_WITH -> "endet";
                            case EQUALS_IGNORE_CASE -> "ohne Beachtung der Groß- und Kleinschreibung entspricht";
                        }).withPreservedWhitespace()
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return switch (mode) {
            case CONTAINS -> "#0 enthält #1";
            case STARTS_WITH -> "#0 beginnt mit #1";
            case ENDS_WITH -> "#0 endet mit #1";
            case EQUALS_IGNORE_CASE -> "#0 ist gleich #1 (ohne Beachtung der Groß- und Kleinschreibung)";
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

    @Nonnull
    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }
}
