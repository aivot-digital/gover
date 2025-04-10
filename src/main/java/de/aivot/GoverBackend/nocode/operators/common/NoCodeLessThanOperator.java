package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeParameterOption;

public class NoCodeLessThanOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "less-than";
    }

    @Override
    public String getLabel() {
        return "Weniger als";
    }

    @Override
    public String getAbstract() {
        return "Vergleicht zwei Werte miteinander, um festzustellen, ob der erste Wert kleiner als der zweite Wert ist.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Weniger als“** vergleicht zwei Werte und prüft, ob der erste Wert kleiner als der zweite Wert ist. \s
                Das Ergebnis ist **wahr** (true), wenn der erste Wert kleiner ist als der zweite Wert. \s
                Andernfalls ist das Ergebnis **falsch**.

                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob eine Zahl kleiner als eine andere Zahl ist.

                Mit dem Operator **„Weniger als“** wird diese Logik so formuliert: \s
                `Wert1 WENIGER ALS Wert2`

                **Ergebnis:**
                - **Bedingung wahr:** Wenn Wert1 kleiner ist als Wert2, ist das Ergebnis **wahr**. \s
                - **Bedingung falsch:** Wenn Wert1 nicht kleiner ist als Wert2, ist das Ergebnis **falsch**.

                # Wahrheitswerte für den „Weniger als“-Operator
                - **5 WENIGER ALS 10** → **wahr** \s
                - **10 WENIGER ALS 5** → **falsch** \s
                - **"abc" WENIGER ALS "abcd"** → **wahr** (länge des Textes) \s
                - **"abcd" WENIGER ALS "abc"** → **falsch** (länge des Textes)

                # Wann verwenden Sie den Operator „Weniger als“?
                Verwenden Sie **„Weniger als“**, wenn Sie prüfen möchten, ob ein Wert kleiner ist als ein anderer Wert. \s
                Dieser Operator ist besonders hilfreich bei Vergleichen von Zahlen, Datumswerten oder Zeichenfolgen.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Any,
                        "Wert 1"
                ),
                new NoCodeParameter(
                        NoCodeDataType.Any,
                        "Wert 2"
                ),
        };
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.Boolean;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        var arg0 = castToNumber(args[0]);
        var arg1 = castToNumber(args[1]);

        return new NoCodeResult(NoCodeDataType.Boolean, arg0.compareTo(arg1) < 0);
    }
}