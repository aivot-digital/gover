package de.aivot.GoverBackend.nocode.operators.common;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeParameterOption;

public class NoCodeIfOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "if";
    }

    @Override
    public String getLabel() {
        return "Wenn Dann Sonst";
    }

    @Override
    public String getAbstract() {
        return "Überprüft einen Wahrheitswert und gibt basierend auf diesem entweder den zweiten Parameter (wenn die Bedingung wahr ist) oder den dritten Parameter (wenn die Bedingung falsch ist) zurück.";
    }

    @Override
    public String getDescription() {
        return """                
                # Beschreibung:
                Der Operator **„Wenn Dann Sonst“** überprüft einen Wahrheitswert (true oder false) und gibt basierend auf diesem entweder den **zweiten Parameter** (wenn die Bedingung wahr ist) oder den **dritten Parameter** (wenn die Bedingung falsch ist) zurück.
                
                - Ist die Bedingung **wahr**, wird der **zweite Parameter** zurückgegeben. \s
                - Ist die Bedingung **falsch**, wird der **dritte Parameter** zurückgegeben.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten eine Nachricht generieren, die vom Status einer Bewerbung abhängt:
                
                - Wenn die Bewerbung **angenommen** ist, soll die Nachricht „Herzlichen Glückwunsch“ lauten. \s
                - Andernfalls soll die Nachricht „Leider abgelehnt“ sein.
                
                Mit dem Operator **„Wenn Dann Sonst“** wird diese Logik so formuliert: \s
                `WENN Bewerbung Angenommen DANN "Herzlichen Glückwunsch" SONST "Leider abgelehnt"`
                
                **Ergebnis:**
                - **Bedingung wahr:** Wenn die Bewerbung **angenommen** ist, gibt der Operator den Text **„Herzlichen Glückwunsch“** zurück.
                - **Bedingung falsch:** Wenn die Bewerbung **abgelehnt** ist, gibt der Operator den Text **„Leider abgelehnt“** zurück.
                
                # Wahrheitswerte für den „Wenn Dann Sonst“-Operator
                - **WENN wahr DANN Wert1 SONST Wert2** → **Wert1** \s
                - **WENN falsch DANN Wert1 SONST Wert2** → **Wert2**
                
                # Wann verwenden Sie den Operator „Wenn Dann Sonst“?
                Verwenden Sie **„Wenn Dann Sonst“**, wenn Sie zwischen zwei Ergebnissen basierend auf einer Bedingung auswählen möchten. \s
                Dieser Operator ist besonders hilfreich für bedingte Logik, z. B. um personalisierte Nachrichten, Optionen oder Werte abhängig von einer Entscheidung oder einem Zustand zu erzeugen.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Boolean,
                        "Wahrheitswert",
                        new NoCodeParameterOption("Wahr", "true"),
                        new NoCodeParameterOption("Falsch", "false")
                ),
                new NoCodeParameter(
                        NoCodeDataType.Any,
                        "Ergebnis bei Wahr"
                ),
                new NoCodeParameter(
                        NoCodeDataType.Any,
                        "Ergebnis bei Falsch"
                ),
        };
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.Any;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        var arg0 = castToBoolean(args[0]);
        var arg1 = args[1];
        var arg2 = args[2];

        return new NoCodeResult(NoCodeDataType.Any, arg0 ? arg1 : arg2);
    }
}
