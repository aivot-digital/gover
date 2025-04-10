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

public class NoCodeRoundUpOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "round-up";
    }

    @Override
    public String getLabel() {
        return "Aufrunden";
    }

    @Override
    public String getAbstract() {
        return "Rundet eine Zahl auf die angegebene Anzahl von Dezimalstellen nach oben auf.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Aufrunden“** rundet eine gegebene Zahl auf die angegebene Anzahl von Dezimalstellen **nach oben** auf. \s
                Das bedeutet, dass der Wert unabhängig vom nächsten Dezimalwert immer aufgerundet wird.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie haben die Zahl `3.14159` und möchten diese auf 2 Dezimalstellen aufrunden.
                
                Mit dem Operator **„Aufrunden“** formulieren Sie: \s
                ```text
                Aufrunden(3.14159, 2)
                ```
                
                **Ergebnis:** \s
                Die gerundete Zahl lautet: `3.15`
                
                **Weitere Beispiele:**
                - **Aufrunden(4.123, 1):** → **4.2** \s
                - **Aufrunden(5.6, 0):** → **6** (auf ganze Zahl gerundet) \s
                - **Aufrunden(2.987, 3):** → **2.987** (keine Veränderung, da bereits 3 Dezimalstellen vorhanden sind)
                
                # Wann verwenden Sie den Operator „Aufrunden“?
                Verwenden Sie **„Aufrunden“**, wenn:
                - Sie präzise Werte auf eine definierte Anzahl von Dezimalstellen benötigen. \s
                - Berechnungen oder Ergebnisse immer **nach oben** gerundet werden sollen, z. B. bei Preisberechnungen, die keine Rundungsverluste erlauben. \s
                - Sie genaue, aufgerundete Werte für Berichte oder Darstellungen benötigen.
                
                Dieser Operator ist besonders hilfreich in Finanzberechnungen, wissenschaftlichen Analysen oder wenn garantierte Obergrenzen eingehalten werden müssen.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Number,
                        "Wert"
                ),
                new NoCodeParameter(
                        NoCodeDataType.Number,
                        "Dezimalstellen"
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

        var number = castToNumber(args[0]);
        var decimalPlaces = castToNumber(args[1]).intValue();

        return new NoCodeResult(NoCodeDataType.Number, number.setScale(decimalPlaces, RoundingMode.HALF_UP));
    }
}
