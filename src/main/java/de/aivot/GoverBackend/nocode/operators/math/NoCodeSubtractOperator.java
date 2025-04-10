package de.aivot.GoverBackend.nocode.operators.math;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

public class NoCodeSubtractOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "subtract";
    }

    @Override
    public String getLabel() {
        return "Subtrahiere";
    }

    @Override
    public String getAbstract() {
        return "Subtrahiert zwei Werte.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Subtrahiere“** zieht einen numerischen Wert von einem anderen ab. \s
                Das Ergebnis ist die Differenz der beiden Werte.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten die Differenz von zwei Zahlen berechnen.
                
                Mit dem Operator **„Subtrahiere“** wird diese Logik so formuliert: \s
                `Subtrahiere Wert1 Wert2`
                
                Beispielwerte: \s
                - **Wert1:** 10 \s
                - **Wert2:** 3
                
                **Ergebnis:**
                - **Rückgabewert:** 7
                
                # Wahrheitswerte für den „Subtrahiere“-Operator
                - **Subtrahiere 10 2** → **8** \s
                - **Subtrahiere 9 3** → **6** \s
                - **Subtrahiere 5 10** → **-5**
                
                # Wann verwenden Sie den Operator „Subtrahiere“?
                Verwenden Sie **„Subtrahiere“**, um die Differenz von zwei numerischen Werten zu berechnen. \s
                Dieser Operator ist besonders hilfreich bei mathematischen Berechnungen, Finanzanalysen oder statistischen Auswertungen.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Number,
                        "Minuend"
                ),
                new NoCodeParameter(
                        NoCodeDataType.Number,
                        "Subtrahend"
                ),
        };
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.Number;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        if (args.length < 2) {
            throw new NoCodeWrongArgumentCountException(2, args.length);
        }

        var result = castToNumber(args[0]);

        for (int i = 1; i < args.length; i++) {
            var minuend = castToNumber(args[i]);
            result = result.subtract(minuend);
        }

        return new NoCodeResult(NoCodeDataType.Number, result);
    }
}