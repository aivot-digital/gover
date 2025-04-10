package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

public class NoCodeNotEqualsOperator extends NoCodeEqualsOperator {
    @Override
    public String getIdentifier() {
        return "not-equals";
    }

    @Override
    public String getLabel() {
        return "Ist nicht Gleich";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob zwei Werte nicht gleich sind.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Ist Nicht Gleich“** prüft, ob zwei Werte **nicht gleich** sind. \s
                Das Ergebnis ist **wahr** (true), wenn die beiden Werte **unterschiedlich** sind. \s
                Sind die beiden Werte identisch, ergibt der Operator den Wert **falsch**.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob die eingegebene Postleitzahl **nicht** der erwarteten Postleitzahl entspricht.
                
                Mit dem Operator **„Ist Nicht Gleich“** wird diese Logik so formuliert: \s
                `Eingabe Postleitzahl IST NICHT GLEICH Erwartete Postleitzahl`
                
                **Ergebnis:**
                - **Bedingung wahr:** Wenn die eingegebene Postleitzahl von der erwarteten Postleitzahl abweicht, ist das Ergebnis **wahr**. \s
                - **Bedingung falsch:** Wenn die eingegebene Postleitzahl der erwarteten Postleitzahl entspricht, ist das Ergebnis **falsch**.
                
                # Wahrheitswerte für den „Ist Nicht Gleich“-Operator
                - **10 IST NICHT GLEICH 20** → **wahr** \s
                - **10 IST NICHT GLEICH 10** → **falsch** \s
                - **"Hallo" IST NICHT GLEICH "Welt"** → **wahr** \s
                - **"Hallo" IST NICHT GLEICH "Hallo"** → **falsch**
                
                # Wann verwenden Sie den Operator „Ist Nicht Gleich“?
                Verwenden Sie **„Ist Nicht Gleich“**, wenn Sie prüfen möchten, ob zwei Werte **nicht identisch** sind. \s
                Dieser Operator ist besonders hilfreich, um Abweichungen zu erkennen, z. B. bei Validierungen, Vergleichen oder wenn eine Übereinstimmung ausgeschlossen werden soll.
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
