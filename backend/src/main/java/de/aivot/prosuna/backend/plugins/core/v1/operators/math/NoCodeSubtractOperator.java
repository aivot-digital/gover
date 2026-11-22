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
        return "Zieht eine Zahl von einer anderen ab.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Ziehen Sie eine Zahl von einer anderen ab, etwa einen bereits gezahlten Betrag von einer Gesamtsumme.

                # Welche Werte werden benötigt?
                „Ausgangszahl“ und „Abzuziehende Zahl“. Beide können Sie frei eingeben; es gibt keine vorgegebenen Auswahlwerte. Über „Andere Wertquelle verwenden …“ können Sie Zahlen aus Wertquellen oder Funktionen übernehmen.

                # Welches Ergebnis erhalten Sie?
                Die Ausgangszahl abzüglich der abzuziehenden Zahl.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Subtrahieren“ als Funktion für einen Zahlenwert.
                2. Tragen Sie bei „Ausgangszahl“ `10` und bei „Abzuziehende Zahl“ `3` ein.
                3. Verwenden Sie das Ergebnis beispielsweise als berechneten Zahlenwert eines Formularelements.

                Der Ausdruck `10 minus 3` ergibt `7`. Für weitere Rechenschritte können Sie Funktionen verschachteln.

                # Was ist zu beachten?
                Verwenden Sie Zahlen. Die Berechnung rundet Eingaben auf acht Nachkommastellen. Zahlen als Text werden übernommen; andere Texte können anhand ihrer Länge in Zahlen umgewandelt werden. Fehlende Werte werden wie die Zahl `0` behandelt. Die Reihenfolge der Zahlen beeinflusst das Ergebnis.
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
                                "Die Zahl, von der die andere Zahl abgezogen wird."
                        ).withSentence(""),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Abzuziehende Zahl",
                                "Die Zahl, die von der Ausgangszahl abgezogen wird."
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
