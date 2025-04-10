package de.aivot.GoverBackend.nocode.operators.bool;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeParameterOption;

public class NoCodeOrOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "or";
    }

    @Override
    public String getLabel() {
        return "Oder";
    }

    @Override
    public String getAbstract() {
        return "Verknüpft zwei Wahrheitswerte über das logische Oder miteinander.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Oder“** (auch bekannt als logisches ODER) verknüpft zwei Wahrheitswerte miteinander. \s
                Das Ergebnis ist **wahr** (true), wenn **mindestens eine** der verknüpften Bedingungen **wahr** ist. \s
                Ist **keine** der Bedingungen wahr, ergibt der Operator **„Oder“** den Wert **falsch**.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob eine Person die folgenden Bedingungen erfüllt:
                
                - Sie hat einen gültigen Ausweis. \s
                - Oder sie besitzt eine andere Form der Identifikation.
                
                Mit dem Operator **„Oder“** wird diese Logik so formuliert: \s
                `Hat gültigen Ausweis ODER Besitzt andere Identifikation`
                
                **Ergebnis:**
                - Wenn die Person **einen gültigen Ausweis** hat oder **eine andere Identifikation** besitzt, ist das Ergebnis **wahr**.
                - Wenn die Person **keinen gültigen Ausweis** hat und **keine andere Identifikation** besitzt, ist das Ergebnis **falsch**.
                
                # Wahrheitswerte für den „Oder“-Operator
                - **wahr** ODER **wahr** → **wahr**
                - **wahr** ODER **falsch** → **wahr**
                - **falsch** ODER **wahr** → **wahr**
                - **falsch** ODER **falsch** → **falsch**
                
                # Wann verwenden Sie den Operator „Oder“?
                Verwenden Sie **„Oder“**, wenn **mindestens eine Bedingung erfüllt** sein muss, um die gewünschte Aktion oder Entscheidung zu treffen. \s
                Dieser Operator ist nützlich, wenn Sie mehrere alternative Kriterien zulassen möchten, z. B. bei Optionen oder Auswahlmöglichkeiten.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Boolean,
                        "Wert 1",
                        new NoCodeParameterOption("Wahr", "true"),
                        new NoCodeParameterOption("Falsch", "false")
                ),
                new NoCodeParameter(
                        NoCodeDataType.Boolean,
                        "Wert 2",
                        new NoCodeParameterOption("Wahr", "true"),
                        new NoCodeParameterOption("Falsch", "false")
                ),
        };
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.Boolean;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        var arg0 = castToBoolean(args[0]);
        var arg1 = castToBoolean(args[1]);

        return new NoCodeResult(NoCodeDataType.Boolean, arg0 || arg1);
    }
}
