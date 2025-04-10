package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeParameterOption;

public class NoCodeGreaterThanOrEqualOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "greater-than-or-equal";
    }

    @Override
    public String getLabel() {
        return "Größer als oder gleich";
    }

    @Override
    public String getAbstract() {
        return "Vergleicht zwei Werte miteinander, um festzustellen, ob der erste Wert größer oder gleich dem zweiten Wert ist.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Größer als oder gleich“** vergleicht zwei Werte und prüft, ob der erste Wert größer oder gleich dem zweiten Wert ist. \s
                Das Ergebnis ist **wahr** (true), wenn der erste Wert größer oder gleich dem zweiten Wert ist. \s
                Andernfalls ist das Ergebnis **falsch**.

                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob eine Zahl größer oder gleich einer anderen Zahl ist.

                Mit dem Operator **„Größer als oder gleich“** wird diese Logik so formuliert: \s
                `Wert1 GRÖßER ALS ODER GLEICH Wert2`

                **Ergebnis:**
                - **Bedingung wahr:** Wenn Wert1 größer oder gleich Wert2 ist, ist das Ergebnis **wahr**. \s
                - **Bedingung falsch:** Wenn Wert1 kleiner ist als Wert2, ist das Ergebnis **falsch**.

                # Wahrheitswerte für den „Größer als oder gleich“-Operator
                - **10 GRÖßER ALS ODER GLEICH 5** → **wahr** \s
                - **5 GRÖßER ALS ODER GLEICH 10** → **falsch** \s
                - **10 GRÖßER ALS ODER GLEICH 10** → **wahr** \s
                - **"abcd" GRÖßER ALS ODER GLEICH "abc"** → **wahr** (länge des Textes) \s
                - **"abc" GRÖßER ALS ODER GLEICH "abcd"** → **falsch** (länge des Textes) \s
                - **"abcd" GRÖßER ALS ODER GLEICH "abcd"** → **wahr** (länge des Textes)

                # Wann verwenden Sie den Operator „Größer als oder gleich“?
                Verwenden Sie **„Größer als oder gleich“**, wenn Sie prüfen möchten, ob ein Wert größer oder gleich einem anderen Wert ist. \s
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

        return new NoCodeResult(NoCodeDataType.Boolean, arg0.compareTo(arg1) >= 0);
    }
}