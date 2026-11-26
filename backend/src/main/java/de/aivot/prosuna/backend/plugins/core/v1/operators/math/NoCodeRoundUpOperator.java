package de.aivot.prosuna.backend.plugins.core.v1.operators.math;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.math.RoundingMode;

public class NoCodeRoundUpOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "round-up";
    }

    @Override
    public String getLabel() {
        return "Runden (bei gleichem Abstand von null weg)";
    }

    @Override
    public String getAbstract() {
        return "Rundet zur nächstgelegenen Zahl; bei gleichem Abstand weg von null.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Runden Sie eine Zahl auf die gewünschte Anzahl von Nachkommastellen. Liegt die Zahl genau zwischen zwei möglichen Ergebnissen, wird das weiter von null entfernte Ergebnis gewählt.

                # Welche Werte werden benötigt?

                - **Zahl:** Die Zahl, die gerundet wird.
                - **Nachkommastellen:** Die gewünschte Anzahl der Nachkommastellen.

                Beide können Sie frei eingeben; es gibt keine vorgegebenen Auswahlwerte. Über **„Andere Wertquelle verwenden …“** können die Eingaben aus Wertquellen oder Funktionen stammen.

                # Welches Ergebnis erhalten Sie?

                Die nächstgelegene Zahl mit der gewählten Anzahl an Nachkommastellen. Bei gleichem Abstand wird weg von null gerundet.

                # Beispiel im No-Code-Editor

                1. Wählen Sie **„Runden (bei gleichem Abstand von null weg)“** als Funktion für einen Zahlenwert.
                2. Tragen Sie bei **„Zahl“** `2.345` und bei **„Nachkommastellen“** `2` ein.
                3. Verwenden Sie das Ergebnis beispielsweise als berechneten Zahlenwert.

                Das Ergebnis ist `2.35`. Bei `2.346` erhalten Sie mit beiden Rundungsfunktionen `2.35`, bei `2.344` jeweils `2.34`.

                # Was ist zu beachten?

                Es wird nicht grundsätzlich aufgerundet. Die Rundungsrichtung unterscheidet sich nur bei genau gleichem Abstand zu beiden Ergebnissen. Auch negative Zahlen werden dann weg von null gerundet: `-2.345` ergibt bei zwei Nachkommastellen `-2.35`. Mit `0` Nachkommastellen wird auf ganze Zahlen gerundet, mit negativen Angaben auf Zehner, Hunderter usw.: `2345` ergibt bei `-2` Nachkommastellen `2300`. Für **„Nachkommastellen“** ist eine ganze Zahl von `-15` bis `15` erforderlich. Als **„Zahl“** können Sie auch Text wie `2.345` oder `2,345` verwenden. Ein fehlender Wert und leerer Text gelten als `0`; andere Texte und Werte führen zu einer Fehlermeldung.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Number,
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Zahl",
                                "Die Zahl, die zur nächstgelegenen Zahl gerundet wird."
                        ).withSentence(""),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Nachkommastellen",
                                "Die gewünschte Anzahl an Nachkommastellen von -15 bis 15; 0 rundet auf ganze Zahlen, negative Werte auf Zehner, Hunderter usw."
                        ).withSentence("auf", "Nachkommastellen gerundet (bei gleichem Abstand von null weg)")
                                .withNumberConstraints(RoundOperatorSupport.DECIMAL_PLACES)
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 auf #1 Nachkommastellen gerundet (bei gleichem Abstand von null weg)";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length < 2) {
            throw new NoCodeWrongArgumentCountException(2, args.length);
        }

        var number = castToNumber(args[0]);
        var decimalPlaces = requireInteger(args[1], "Geben Sie für „Nachkommastellen“ eine ganze Zahl im unterstützten Zahlenbereich ein.");

        return new NoCodeResult(RoundOperatorSupport.round(number, decimalPlaces, RoundingMode.HALF_UP));
    }
}
