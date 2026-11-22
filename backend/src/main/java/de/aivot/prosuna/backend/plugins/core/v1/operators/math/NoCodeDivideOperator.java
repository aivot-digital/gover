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
        return "Teilt eine Ausgangszahl der Reihe nach durch einen oder mehrere Teiler.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Verteilen Sie einen Gesamtbetrag gleichmäßig, beispielsweise zuerst auf mehrere Gruppen und anschließend auf gleich viele Personen je Gruppe.

                # Welche Werte werden benötigt?
                Sie benötigen eine „Ausgangszahl“ und mindestens einen Teiler. Jeder Teiler muss von null verschieden sein. Jede Zahl können Sie frei eingeben oder über „Andere Wertquelle verwenden …“ aus einer Wertquelle oder Funktion übernehmen. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                Die Funktion teilt die Ausgangszahl durch den ersten Teiler und anschließend jedes Zwischenergebnis durch den nächsten Teiler. Nach jedem Schritt rundet sie auf acht Nachkommastellen.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Dividieren“ als Funktion für einen Zahlenwert.
                2. Tragen Sie für einen Betrag von 24 Euro bei „Ausgangszahl“ `24` ein. Geben Sie bei „Teiler 1“ für drei Gruppen `3` ein.
                3. Wählen Sie „Weitere Zahl hinzufügen“ und geben Sie bei „Teiler 2“ für zwei Personen je Gruppe `2` ein.

                Der Ausdruck `24 geteilt durch 3 geteilt durch 2` ergibt `4`. Auf jede Person entfallen 4 Euro.

                # Was ist zu beachten?
                Verwenden Sie Zahlen. Die Berechnung rundet Eingaben auf acht Nachkommastellen. Zahlen als Text werden übernommen; andere Texte können anhand ihrer Länge in Zahlen umgewandelt werden. Fehlende Werte werden wie die Zahl `0` behandelt. Ein fehlender Teiler führt deshalb zu einer Fehlermeldung.
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
                                "Teiler 1",
                                "Die Zahl, durch die das bisherige Ergebnis geteilt wird. Sie darf nicht 0 sein."
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
