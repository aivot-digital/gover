package de.aivot.GoverBackend.nocode.operators.math;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

import java.math.BigDecimal;

public class NoCodeAddOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "add";
    }

    @Override
    public String getLabel() {
        return "Addiere";
    }

    @Override
    public String getAbstract() {
        return "Addiert zwei Werte.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Addiere“** summiert zwei oder mehr numerische Werte. \s
                Das Ergebnis ist die Summe aller angegebenen Werte.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten die Summe von zwei Zahlen berechnen.
                
                Mit dem Operator **„Addiere“** wird diese Logik so formuliert: \s
                `Addiere Wert1 Wert2`
                
                Beispielwerte: \s
                - **Wert1:** 5 \s
                - **Wert2:** 3
                
                **Ergebnis:**
                - **Rückgabewert:** 8
                
                # Wahrheitswerte für den „Addiere“-Operator
                - **Addiere 1 2** → **3** \s
                - **Addiere 10 20 30** → **60** \s
                - **Addiere -5 5** → **0**
                
                # Wann verwenden Sie den Operator „Addiere“?
                Verwenden Sie **„Addiere“**, um die Summe von zwei oder mehr numerischen Werten zu berechnen. \s
                Dieser Operator ist besonders hilfreich bei mathematischen Berechnungen, Finanzanalysen oder statistischen Auswertungen.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Number,
                        "Summand 1"
                ),
                new NoCodeParameter(
                        NoCodeDataType.Number,
                        "Summand 2"
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

        BigDecimal sum = BigDecimal.ZERO;
        for (Object rawArg : args) {
            var arg = castToNumber(rawArg);
            sum = sum.add(arg);
        }

        return new NoCodeResult(NoCodeDataType.Number, sum);
    }
}
