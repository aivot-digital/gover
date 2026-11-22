package de.aivot.prosuna.backend.plugins.core.v1.operators.math;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public class NoCodeAddOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "add";
    }

    @Override
    public String getLabel() {
        return "Addieren";
    }

    @Override
    public String getAbstract() {
        return "Bildet die Summe aus zwei oder mehr Zahlen.";
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 plus #1";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Berechnen Sie einen Gesamtwert, etwa die Summe mehrerer Einzelbeträge.

                # Welche Werte werden benötigt?
                Sie benötigen mindestens zwei Zahlen. Jede Zahl können Sie frei eingeben oder über „Andere Wertquelle verwenden …“ aus einer Wertquelle oder Funktion übernehmen. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                Die Summe aller angegebenen Zahlen.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Addieren“ als Funktion für einen Zahlenwert.
                2. Tragen Sie bei „Zahl 1“ `5` und bei „Zahl 2“ `3` ein.
                3. Wählen Sie „Weitere Zahl hinzufügen“ und geben Sie bei „Zahl 3“ `2` ein.

                Der Ausdruck `5 plus 3 plus 2` ergibt `10`. Verwenden Sie die Summe beispielsweise als berechneten Wert eines Formularelements.

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
                                "Die erste Zahl für die Summe."
                        ).withSentence(""),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Zahl 2",
                                "Die Zahl, die zur bisherigen Summe hinzukommt."
                        ).withSentence("plus")
                )
        );
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

        BigDecimal sum = BigDecimal.ZERO;
        for (Object rawArg : args) {
            var arg = castToNumber(rawArg);
            sum = sum.add(arg);
        }

        return new NoCodeResult(sum);
    }
}
