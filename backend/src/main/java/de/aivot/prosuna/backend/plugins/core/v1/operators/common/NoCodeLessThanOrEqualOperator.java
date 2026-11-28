package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import org.jetbrains.annotations.Nullable;

public class NoCodeLessThanOrEqualOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "less-than-or-equal";
    }

    @Override
    public String getLabel() {
        return "Kleiner als oder gleich";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob eine Zahl die Vergleichszahl erreicht oder unterschreitet.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Prüfen Sie, ob eine Zahl die Vergleichszahl erreicht oder unterschreitet, etwa beim Vergleich einer Menge mit einem Grenzwert.

                # Welche Werte werden benötigt?

                - **Zahl:** Die Zahl, die Sie prüfen möchten.
                - **Vergleichszahl:** Die Zahl, mit der Sie die erste Zahl vergleichen möchten.

                Beide können Sie frei eingeben; es gibt keine vorgegebenen Auswahlwerte. Über **„Andere Wertquelle verwenden …“** können Sie Zahlen aus Wertquellen oder Funktionen übernehmen.

                # Welches Ergebnis erhalten Sie?

                Ein Wahrheitswert: `Wahr`, wenn die erste Zahl die Vergleichszahl erreicht oder unterschreitet, andernfalls `Falsch`.

                # Beispiel im No-Code-Editor

                1. Wählen Sie **„Kleiner als oder gleich“** als Funktion für eine Bedingung.
                2. Tragen Sie bei **„Zahl“** `10` und bei **„Vergleichszahl“** `10` ein.
                3. Verwenden Sie die Bedingung beispielsweise für die Sichtbarkeit eines Formularelements.

                Der Ausdruck `10 ist kleiner als oder gleich 10` ergibt `Wahr`. Statt der ersten festen Zahl können Sie eine passende Wertquelle wählen.

                # Was ist zu beachten?

                Verwenden Sie Zahlen. Zahlen als Text werden numerisch verglichen, mit Dezimalpunkt oder Dezimalkomma wie `1.5` oder `1,5`; Tausendertrennzeichen werden nicht unterstützt. Fehlende Werte und leerer Text werden wie die Zahl `0` behandelt. Andere Werte, etwa sonstige Texte, Wahrheitswerte oder Datumswerte, führen zu einer Fehlermeldung. Für fachliche Datums-, Uhrzeit- oder Zeitpunktvergleiche verwenden Sie die dafür vorgesehenen Funktionen.
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
                        ).withSentence("kleiner als oder gleich", "ist")
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var arg0 = castToNumber(args[0]);
        var arg1 = castToNumber(args[1]);

        return new NoCodeResult(arg0.compareTo(arg1) <= 0);
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 ist kleiner als oder gleich #1";
    }
}
