package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeParameterOption;

public class NoCodeIsDefinedOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "is-defined";
    }

    @Override
    public String getLabel() {
        return "Ist nicht Leer";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob ein gegebener Wert nicht leer ist.";
    }

    @Override
    public String getDescription() {
        return """                
                # Beschreibung:
                Der Operator **„Ist Nicht Leer“** prüft, ob ein gegebener Wert **nicht leer** ist. \s
                Das Ergebnis ist **wahr** (true), wenn der Wert vorhanden ist, also nicht leer, null oder undefiniert. \s
                Ist der Wert leer (z. B. ein leeres Feld, null oder ein nicht gesetzter Wert), ergibt der Operator den Wert **falsch**.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob ein Benutzer eine Telefonnummer angegeben hat.
                
                Mit dem Operator **„Ist Nicht Leer“** wird diese Logik so formuliert: \s
                `IST NICHT LEER Telefonnummer`
                
                **Ergebnis:**
                - **Bedingung wahr:** Wenn eine Telefonnummer angegeben ist, ist das Ergebnis **wahr**. \s
                - **Bedingung falsch:** Wenn das Feld leer ist, ist das Ergebnis **falsch**.
                
                # Wahrheitswerte für den „Ist Nicht Leer“-Operator
                - **IST NICHT LEER Hallo** → **wahr** \s
                - **IST NICHT LEER ""** (leerer Text) → **falsch** \s
                - **IST NICHT LEER 123** → **wahr** \s
                - **IST NICHT LEER null** → **falsch**
                
                # Wann verwenden Sie den Operator „Ist Nicht Leer“?
                Verwenden Sie **„Ist Nicht Leer“**, um sicherzustellen, dass ein Wert vorhanden ist, bevor Sie damit arbeiten. \s
                Dieser Operator ist besonders hilfreich bei Formularen, um zu prüfen, ob ein Benutzer Pflichtfelder ausgefüllt hat oder ob wichtige Daten bereitstehen.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Any,
                        "Wert"
                ),
        };
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.Boolean;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        return new NoCodeResult(NoCodeDataType.Boolean, args[0] != null);
    }
}
