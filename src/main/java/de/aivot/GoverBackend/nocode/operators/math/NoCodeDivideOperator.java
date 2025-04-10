package de.aivot.GoverBackend.nocode.operators.math;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NoCodeDivideOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "divide";
    }

    @Override
    public String getLabel() {
        return "Dividiere";
    }

    @Override
    public String getAbstract() {
        return "Dividiert zwei Werte.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Dividiere“** teilt einen numerischen Wert durch einen anderen. \s
                Das Ergebnis ist der Quotient der beiden Werte.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten den Quotienten von zwei Zahlen berechnen.
                
                Mit dem Operator **„Dividiere“** wird diese Logik so formuliert: \s
                `Dividiere Wert1 Wert2`
                
                Beispielwerte: \s
                - **Wert1:** 10 \s
                - **Wert2:** 2
                
                **Ergebnis:**
                - **Rückgabewert:** 5
                
                # Wahrheitswerte für den „Dividiere“-Operator
                - **Dividiere 10 2** → **5** \s
                - **Dividiere 9 3** → **3** \s
                - **Dividiere 5 0** → **unendlich** (Division durch Null)
                
                # Wann verwenden Sie den Operator „Dividiere“?
                Verwenden Sie **„Dividiere“**, um den Quotienten von zwei numerischen Werten zu berechnen. \s
                Dieser Operator ist besonders hilfreich bei mathematischen Berechnungen, Finanzanalysen oder statistischen Auswertungen.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Number,
                        "Dividend"
                ),
                new NoCodeParameter(
                        NoCodeDataType.Number,
                        "Divisor"
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

        var dividend = castToNumber(args[0]);

        for (int i = 1; i < args.length; i++) {
            var divisor = castToNumber(args[i]);
            if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                throw new NoCodeException("Division durch Null ist nicht erlaubt");
            }

            dividend = dividend.divide(divisor, RoundingMode.HALF_UP);
        }

        return new NoCodeResult(NoCodeDataType.Number, dividend);
    }
}