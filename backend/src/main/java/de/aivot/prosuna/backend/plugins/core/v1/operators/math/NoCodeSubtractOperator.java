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

public class NoCodeSubtractOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "subtract";
    }

    @Override
    public String getLabel() {
        return "Subtrahieren";
    }

    @Override
    public String getAbstract() {
        return "Zieht eine oder mehrere Zahlen von einer Ausgangszahl ab.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Berechnen Sie einen Restbetrag, indem Sie bereits gezahlte Beträge von der Gesamtsumme abziehen.

                # Welche Werte werden benötigt?
                Sie benötigen eine „Ausgangszahl“ und mindestens eine Zahl, die Sie davon abziehen möchten. Jede Zahl können Sie frei eingeben oder über „Andere Wertquelle verwenden …“ aus einer Wertquelle oder Funktion übernehmen. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                Die Ausgangszahl abzüglich aller weiteren Zahlen. Jede weitere Zahl wird vom bisherigen Ergebnis abgezogen.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Subtrahieren“ als Funktion für einen Zahlenwert.
                2. Tragen Sie bei „Ausgangszahl“ `20` und bei „Abzuziehende Zahl 1“ `5` ein.
                3. Wählen Sie „Weitere Zahl hinzufügen“ und geben Sie bei „Abzuziehende Zahl 2“ `3` ein.

                Der Ausdruck `20 minus 5 minus 3` ergibt `12`. Verwenden Sie das Ergebnis beispielsweise als offenen Restbetrag.

                # Was ist zu beachten?
                Verwenden Sie Zahlen. Zahlen als Text werden übernommen, mit Dezimalpunkt oder Dezimalkomma wie `1.5` oder `1,5`; Tausendertrennzeichen werden nicht unterstützt. Fehlende Werte und leerer Text werden wie die Zahl `0` behandelt. Andere Werte, etwa sonstige Texte, Wahrheitswerte oder Datumswerte, führen zu einer Fehlermeldung. Die Reihenfolge der Zahlen beeinflusst das Ergebnis.
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
                                "Die Zahl, von der die weiteren Zahlen abgezogen werden."
                        ).withSentence(""),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Abzuziehende Zahl 1",
                                "Die Zahl, die vom bisherigen Ergebnis abgezogen wird."
                        ).withSentence("minus")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 minus #1";
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

        var result = castToNumber(args[0]);

        for (int i = 1; i < args.length; i++) {
            var minuend = castToNumber(args[i]);
            result = result.subtract(minuend);
        }

        return new NoCodeResult(result);
    }
}
