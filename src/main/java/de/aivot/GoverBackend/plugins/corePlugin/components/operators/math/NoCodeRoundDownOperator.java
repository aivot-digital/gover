package de.aivot.GoverBackend.plugins.corePlugin.components.operators.math;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;

import java.math.RoundingMode;

public class NoCodeRoundDownOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "round-down";
    }

    @Override
    public String getLabel() {
        return "Abrunden";
    }

    @Override
    public String getAbstract() {
        return "Rundet eine Zahl auf eine definierte Anzahl von Dezimalstellen nach unten ab.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Abrunden“** rundet eine gegebene Zahl auf die angegebene Anzahl von Dezimalstellen **nach unten** ab. \s
                Das bedeutet, dass der Wert immer auf den nächstniedrigeren Wert innerhalb der spezifizierten Dezimalstellen gerundet wird.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie haben die Zahl `3.98765` und möchten diese auf 2 Dezimalstellen abrunden.
                
                Mit dem Operator **„Abrunden“** formulieren Sie: \s
                ```text
                Abrunden(3.98765, 2)
                ```
                
                **Ergebnis:** \s
                Die gerundete Zahl lautet: `3.98`
                
                **Weitere Beispiele:**
                - **Abrunden(4.876, 1):** → **4.8** \s
                - **Abrunden(5.6, 0):** → **5** (auf ganze Zahl gerundet) \s
                - **Abrunden(2.345, 3):** → **2.345** (keine Veränderung, da bereits 3 Dezimalstellen vorhanden sind)
                
                # Wann verwenden Sie den Operator „Abrunden“?
                Verwenden Sie **„Abrunden“**, wenn:
                - Sie präzise Werte auf eine definierte Anzahl von Dezimalstellen benötigen. \s
                - Berechnungen oder Ergebnisse immer **nach unten** gerundet werden sollen, z. B. bei konservativen Schätzungen oder Budgetierungen. \s
                - Sie genaue, abgerundete Werte für Berichte, Darstellungen oder finanzielle Berechnungen wünschen.
                
                Dieser Operator ist besonders nützlich für Budgetierungen, Preisberechnungen oder technische Analysen, bei denen der Wert niemals überschritten werden darf.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Number,
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Wert",
                                "Die Zahl, die abgerundet werden soll."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Dezimalstellen",
                                "Die Anzahl der Dezimalstellen, auf die abgerundet werden soll."
                        )
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
        if (args.length < 2) {
            throw new NoCodeWrongArgumentCountException(2, args.length);
        }

        var number = castToNumber(args[0]);
        var decimalPlaces = castToNumber(args[1]).intValue();

        return new NoCodeResult(number.setScale(decimalPlaces, RoundingMode.HALF_DOWN));
    }
}
