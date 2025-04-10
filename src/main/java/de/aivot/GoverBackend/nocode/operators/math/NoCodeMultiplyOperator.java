package de.aivot.GoverBackend.nocode.operators.math;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

import java.math.BigDecimal;

public class NoCodeMultiplyOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "multiply";
    }

    @Override
    public String getLabel() {
        return "Multipliziere";
    }

    @Override
    public String getAbstract() {
        return "Multipliziert zwei Werte.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Multipliziere“** multipliziert zwei oder mehr numerische Werte. \s
                Das Ergebnis ist das Produkt aller angegebenen Werte.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten das Produkt von zwei Zahlen berechnen.
                
                Mit dem Operator **„Multipliziere“** wird diese Logik so formuliert: \s
                `Multipliziere Wert1 Wert2`
                
                Beispielwerte: \s
                - **Wert1:** 5 \s
                - **Wert2:** 3
                
                **Ergebnis:**
                - **Rückgabewert:** 15
                
                # Wahrheitswerte für den „Multipliziere“-Operator
                - **Multipliziere 2 3** → **6** \s
                - **Multipliziere 4 5 6** → **120** \s
                - **Multipliziere -2 3** → **-6**
                
                # Wann verwenden Sie den Operator „Multipliziere“?
                Verwenden Sie **„Multipliziere“**, um das Produkt von zwei oder mehr numerischen Werten zu berechnen. \s
                Dieser Operator ist besonders hilfreich bei mathematischen Berechnungen, Finanzanalysen oder statistischen Auswertungen.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Number,
                        "Faktor 1"
                ),
                new NoCodeParameter(
                        NoCodeDataType.Number,
                        "Faktor 2"
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

        BigDecimal result = BigDecimal.ONE;

        for (Object rawArg : args) {
            var arg = new BigDecimal(rawArg.toString());
            result = result.multiply(arg);
        }

        return new NoCodeResult(NoCodeDataType.Number, result);
    }
}