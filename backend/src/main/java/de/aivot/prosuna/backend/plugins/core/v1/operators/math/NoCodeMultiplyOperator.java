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

import java.math.BigDecimal;

public class NoCodeMultiplyOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "multiply";
    }

    @Override
    public String getLabel() {
        return "Multiplizieren";
    }

    @Override
    public String getAbstract() {
        return "Multipliziert zwei Zahlen.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Berechnen Sie das Produkt zweier Zahlen, etwa einen Gesamtbetrag aus Menge und Einzelpreis.

                # Welche Werte werden benötigt?
                „Zahl 1“ und „Zahl 2“. Beide können Sie frei eingeben; es gibt keine vorgegebenen Auswahlwerte. Über „Andere Wertquelle verwenden …“ können Sie Zahlen aus Wertquellen oder Funktionen übernehmen.

                # Welches Ergebnis erhalten Sie?
                Das Produkt beider Zahlen.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Multiplizieren“ als Funktion für einen Zahlenwert.
                2. Tragen Sie bei „Zahl 1“ `5` und bei „Zahl 2“ `3` ein.
                3. Verwenden Sie das Ergebnis beispielsweise als berechneten Zahlenwert eines Formularelements.

                Der Ausdruck `5 mal 3` ergibt `15`. Für weitere Rechenschritte können Sie Funktionen verschachteln.

                # Was ist zu beachten?
                Verwenden Sie Zahlen. Die Berechnung rundet Eingaben auf acht Nachkommastellen. Zahlen als Text werden übernommen; andere Texte können anhand ihrer Länge in Zahlen umgewandelt werden. Fehlende Werte werden wie die Zahl `0` behandelt.
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
                                "Zahl 1",
                                "Die erste der beiden Zahlen, die miteinander multipliziert werden."
                        ).withSentence(""),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Zahl 2",
                                "Die zweite der beiden Zahlen, die miteinander multipliziert werden."
                        ).withSentence("mal")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 mal #1";
    }

    @Override
    protected boolean supportsVariableArgumentCount() {
        return true;
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length < 2) {
            throw new NoCodeWrongArgumentCountException(2, args.length);
        }

        BigDecimal result = BigDecimal.ONE;

        for (Object rawArg : args) {
            var arg = castToNumber(rawArg);
            result = result.multiply(arg);
        }

        return new NoCodeResult(result);
    }
}
