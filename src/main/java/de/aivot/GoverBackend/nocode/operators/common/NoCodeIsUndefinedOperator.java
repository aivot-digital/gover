package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

public class NoCodeIsUndefinedOperator extends NoCodeIsDefinedOperator {
    @Override
    public String getIdentifier() {
        return "is-undefined";
    }

    @Override
    public String getLabel() {
        return "Ist Leer";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob ein gegebener Wert leer ist.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Ist Leer“** prüft, ob ein gegebener Wert **leer** ist. \s
                Das Ergebnis ist **wahr** (true), wenn der Wert leer, null oder undefiniert ist. \s
                Ist der Wert vorhanden (also nicht leer), ergibt der Operator den Wert **falsch**.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob ein Benutzer keine E-Mail-Adresse angegeben hat.
                
                Mit dem Operator **„Ist Leer“** wird diese Logik so formuliert: \s
                `IST LEER E-Mail-Adresse`
                
                **Ergebnis:**
                - **Bedingung wahr:** Wenn das Feld für die E-Mail-Adresse leer ist, ist das Ergebnis **wahr**. \s
                - **Bedingung falsch:** Wenn eine E-Mail-Adresse angegeben wurde, ist das Ergebnis **falsch**.
                
                # Wahrheitswerte für den „Ist Leer“-Operator
                - **IST LEER ""** (leerer Text) → **wahr** \s
                - **IST LEER null** → **wahr** \s
                - **IST LEER "Hallo"** → **falsch** \s
                - **IST LEER 123** → **falsch**
                
                # Wann verwenden Sie den Operator „Ist Leer“?
                Verwenden Sie **„Ist Leer“**, um zu prüfen, ob ein Wert fehlt oder leer ist. \s
                Dieser Operator ist besonders hilfreich bei Formularen, um zu validieren, ob ein Benutzer erforderliche Felder leer gelassen hat, oder um sicherzustellen, dass bestimmte Daten eingegeben wurden.
                """;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        var res = super.performEvaluation(data, args);
        return new NoCodeResult(
                res.getDataType(),
                !res.getValueAsBoolean()
        );
    }
}
