package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import org.jetbrains.annotations.Nullable;

public class NoCodeGreaterThanOrEqualOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "greater-than-or-equal";
    }

    @Override
    public String getLabel() {
        return "Größer als oder gleich";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob eine Zahl die Vergleichszahl erreicht oder überschreitet.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Prüfen Sie, ob eine Zahl die Vergleichszahl erreicht oder überschreitet, etwa beim Vergleich einer Menge mit einem Grenzwert.

                # Welche Werte werden benötigt?
                „Zahl“ und „Vergleichszahl“. Beide können Sie frei eingeben; es gibt keine vorgegebenen Auswahlwerte. Über „Andere Wertquelle verwenden …“ können Sie Zahlen aus Wertquellen oder Funktionen übernehmen.

                # Welches Ergebnis erhalten Sie?
                Ein Wahrheitswert: „Wahr“, wenn die erste Zahl die Vergleichszahl erreicht oder überschreitet, andernfalls „Falsch“.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Größer als oder gleich“ als Funktion für eine Bedingung.
                2. Tragen Sie bei „Zahl“ `10` und bei „Vergleichszahl“ `10` ein.
                3. Verwenden Sie die Bedingung beispielsweise für die Sichtbarkeit eines Formularelements.

                Der Ausdruck `10 ist größer als oder gleich 10` ergibt „Wahr“. Statt der ersten festen Zahl können Sie eine passende Wertquelle wählen.

                # Was ist zu beachten?
                Verwenden Sie Zahlen. Die Auswertung rundet Eingaben auf acht Nachkommastellen. Zahlen als Text werden numerisch verglichen. Andere Texte können anhand ihrer Länge verglichen werden; eine alphabetische Sortierung findet nicht statt. Fehlende Werte werden wie die Zahl `0` behandelt. Für fachliche Datums-, Uhrzeit- oder Zeitpunktvergleiche verwenden Sie die dafür vorgesehenen Funktionen.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Zahl",
                                "Die Zahl, die mit der Vergleichszahl geprüft wird."
                        ).withSentence("wenn"),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Vergleichszahl",
                                "Die Zahl, mit der verglichen wird."
                        ).withSentence("größer als oder gleich", "ist")
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var arg0 = castToNumber(args[0]);
        var arg1 = castToNumber(args[1]);

        return new NoCodeResult(arg0.compareTo(arg1) >= 0);
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 ist größer als oder gleich #1";
    }
}
