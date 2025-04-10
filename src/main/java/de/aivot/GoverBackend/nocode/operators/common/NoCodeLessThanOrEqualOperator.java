package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

public class NoCodeLessThanOrEqualOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "less-than-or-equal";
    }

    @Override
    public String getLabel() {
        return "Weniger als oder gleich";
    }

    @Override
    public String getAbstract() {
        return "Vergleicht zwei Werte miteinander, um festzustellen, ob der erste Wert kleiner oder gleich dem zweiten Wert ist.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Weniger als oder gleich“** vergleicht zwei Werte und prüft, ob der erste Wert kleiner oder gleich dem zweiten Wert ist. \s
                Das Ergebnis ist **wahr** (true), wenn der erste Wert kleiner oder gleich dem zweiten Wert ist. \s
                Andernfalls ist das Ergebnis **falsch**.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob eine Zahl kleiner oder gleich einer anderen Zahl ist.
                
                Mit dem Operator **„Weniger als oder gleich“** wird diese Logik so formuliert: \s
                `Wert1 WENIGER ALS ODER GLEICH Wert2`
                
                **Ergebnis:**
                - **Bedingung wahr:** Wenn Wert1 kleiner oder gleich Wert2 ist, ist das Ergebnis **wahr**. \s
                - **Bedingung falsch:** Wenn Wert1 größer ist als Wert2, ist das Ergebnis **falsch**.
                
                # Wahrheitswerte für den „Weniger als oder gleich“-Operator
                - **5 WENIGER ALS ODER GLEICH 10** → **wahr** \s
                - **10 WENIGER ALS ODER GLEICH 5** → **falsch** \s
                - **10 WENIGER ALS ODER GLEICH 10** → **wahr** \s
                - **"abc" WENIGER ALS ODER GLEICH "abcd"** → **wahr** (länge des Textes) \s
                - **"abcd" WENIGER ALS ODER GLEICH "abc"** → **falsch** (länge des Textes) \s
                - **"abcd" WENIGER ALS ODER GLEICH "abcd"** → **wahr** (länge des Textes)
                
                # Wann verwenden Sie den Operator „Weniger als oder gleich“?
                Verwenden Sie **„Weniger als oder gleich“**, wenn Sie prüfen möchten, ob ein Wert kleiner oder gleich einem anderen Wert ist. \s
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

        return new NoCodeResult(NoCodeDataType.Boolean, arg0.compareTo(arg1) <= 0);
    }
}