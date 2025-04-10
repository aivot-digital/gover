package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

public class NoCodeGreaterThanOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "greater-than";
    }

    @Override
    public String getLabel() {
        return "Größer als";
    }

    @Override
    public String getAbstract() {
        return "Vergleicht zwei Werte miteinander, um festzustellen, ob der erste Wert größer als der zweite Wert ist.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Größer als“** vergleicht zwei Werte und prüft, ob der erste Wert größer als der zweite Wert ist. \s
                Das Ergebnis ist **wahr** (true), wenn der erste Wert größer ist als der zweite Wert. \s
                Andernfalls ist das Ergebnis **falsch**.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob eine Zahl größer als eine andere Zahl ist.
                
                Mit dem Operator **„Größer als“** wird diese Logik so formuliert: \s
                `Wert1 GRÖßER ALS Wert2`
                
                **Ergebnis:**
                - **Bedingung wahr:** Wenn Wert1 größer ist als Wert2, ist das Ergebnis **wahr**. \s
                - **Bedingung falsch:** Wenn Wert1 nicht größer ist als Wert2, ist das Ergebnis **falsch**.
                
                # Wahrheitswerte für den „Größer als“-Operator
                - **10 GRÖßER ALS 5** → **wahr** \s
                - **5 GRÖßER ALS 10** → **falsch** \s
                - **"def" GRÖßER ALS "abc"** → **wahr** (länge des Textes) \s
                - **"abc" GRÖßER ALS "def"** → **falsch** (länge des Textes)
                
                # Wann verwenden Sie den Operator „Größer als“?
                Verwenden Sie **„Größer als“**, wenn Sie prüfen möchten, ob ein Wert größer ist als ein anderer Wert. \s
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

        return new NoCodeResult(NoCodeDataType.Boolean, arg0.compareTo(arg1) > 0);
    }
}