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
        return "Addiert zwei Zahlen.";
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
                Berechnen Sie die Summe zweier Zahlen, etwa einen Gesamtbetrag aus zwei Einzelbeträgen.

                # Welche Werte werden benötigt?
                „Zahl 1“ und „Zahl 2“. Beide können Sie frei eingeben; es gibt keine vorgegebenen Auswahlwerte. Über „Andere Wertquelle verwenden …“ können Sie Zahlen aus Wertquellen oder Funktionen übernehmen.

                # Welches Ergebnis erhalten Sie?
                Die Summe beider Zahlen.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Addieren“ als Funktion für einen Zahlenwert.
                2. Tragen Sie bei „Zahl 1“ `5` und bei „Zahl 2“ `3` ein.
                3. Verwenden Sie das Ergebnis beispielsweise als berechneten Zahlenwert eines Formularelements.

                Der Ausdruck `5 plus 3` ergibt `8`. Für weitere Rechenschritte können Sie Funktionen verschachteln.

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
                                "Die Zahl, die zur ersten Zahl addiert wird."
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
