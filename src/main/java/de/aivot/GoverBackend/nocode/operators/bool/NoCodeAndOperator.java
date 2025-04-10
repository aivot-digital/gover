package de.aivot.GoverBackend.nocode.operators.bool;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeParameterOption;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

public class NoCodeAndOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "and";
    }

    @Override
    public String getLabel() {
        return "Und";
    }

    @Override
    public String getAbstract() {
        return "Verknüpft zwei Wahrheitswerte über das logische Und miteinander.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Und“** (auch bekannt als logisches UND) verknüpft zwei Wahrheitswerte miteinander. \s
                Das Ergebnis ist **wahr** (true), wenn beide der verknüpften Bedingungen **wahr** sind. \s
                Falls eine der Bedingungen oder beide **falsch** (false) sind, ergibt der Operator **„Und“** den Wert **falsch**.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob eine Person die folgenden Bedingungen gleichzeitig erfüllt:
                
                - Sie ist über 18 Jahre alt.
                - Sie hat einen gültigen Ausweis.
                
                Mit dem Operator „Und“ wird diese Logik so formuliert: \s
                `Alter > 18 UND Hat gültigen Ausweis`.
                
                **Ergebnis:**
                - Wenn die Person **über 18 ist** und einen **gültigen Ausweis besitzt**, ist das Ergebnis **wahr**.
                - Wenn eine oder beide Bedingungen nicht erfüllt sind, ist das Ergebnis **falsch**.
                
                # Wahrheitswerte für den „Und“-Operator
                - **wahr** UND **wahr** → **wahr**
                - **wahr** UND **falsch** → **falsch**
                - **falsch** UND **wahr** → **falsch**
                - **falsch** UND **falsch** → **falsch**
                
                # Wann verwenden Sie den Operator „Und“?
                Verwenden Sie **„Und“**, wenn alle Bedingungen erfüllt sein müssen, um die gewünschte Aktion oder Entscheidung zu treffen. \s
                Dies ist hilfreich, wenn Sie z. B. Formulare oder Entscheidungen erstellen, bei denen mehrere Kriterien gleichzeitig erfüllt sein müssen.
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

        return new NoCodeResult(NoCodeDataType.Boolean, arg0 && arg1);
    }
}
