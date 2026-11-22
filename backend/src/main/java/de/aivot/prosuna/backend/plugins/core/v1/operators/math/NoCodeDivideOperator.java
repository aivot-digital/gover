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
import java.math.RoundingMode;

public class NoCodeDivideOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "divide";
    }

    @Override
    public String getLabel() {
        return "Dividieren";
    }

    @Override
    public String getAbstract() {
        return "Teilt eine Zahl durch eine andere.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Teilen Sie eine Zahl durch eine andere, etwa einen Gesamtbetrag durch eine Anzahl.

                # Welche Werte werden benötigt?
                „Ausgangszahl“ und „Teiler“. Beide können Sie frei eingeben; es gibt keine vorgegebenen Auswahlwerte. Über „Andere Wertquelle verwenden …“ können Sie Zahlen aus Wertquellen oder Funktionen übernehmen.

                # Welches Ergebnis erhalten Sie?
                Die Ausgangszahl geteilt durch den Teiler. Das Ergebnis wird auf acht Nachkommastellen gerundet.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Dividieren“ als Funktion für einen Zahlenwert.
                2. Tragen Sie bei „Ausgangszahl“ `10` und bei „Teiler“ `2` ein.
                3. Verwenden Sie das Ergebnis beispielsweise als berechneten Zahlenwert eines Formularelements.

                Der Ausdruck `10 geteilt durch 2` ergibt `5`. Für weitere Rechenschritte können Sie Funktionen verschachteln.

                # Was ist zu beachten?
                Verwenden Sie Zahlen. Die Berechnung rundet Eingaben auf acht Nachkommastellen. Zahlen als Text werden übernommen; andere Texte können anhand ihrer Länge in Zahlen umgewandelt werden. Fehlende Werte werden wie die Zahl `0` behandelt. Der Teiler darf nicht `0` sein. Bei mehreren bereits vorhandenen Teilern wird der Reihe nach geteilt und nach jedem Schritt gerundet.
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
                                "Ausgangszahl",
                                "Die Zahl, die geteilt wird."
                        ).withSentence(""),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Teiler",
                                "Die Zahl, durch die geteilt wird. Sie darf nicht 0 sein."
                        ).withSentence("geteilt durch")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 geteilt durch #1";
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

        var dividend = castToNumber(args[0]);

        for (int i = 1; i < args.length; i++) {
            var divisor = castToNumber(args[i]);
            if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                throw new NoCodeException("„Teiler“ darf nicht 0 sein. Geben Sie eine andere Zahl ein.");
            }

            dividend = dividend.divide(divisor, RoundingMode.HALF_UP);
        }

        return new NoCodeResult(dividend);
    }
}
