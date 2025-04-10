package de.aivot.GoverBackend.nocode.operators.bool;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeParameterOption;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

public class NoCodeNotOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "not";
    }

    @Override
    public String getLabel() {
        return "Nicht";
    }

    @Override
    public String getAbstract() {
        return "Kehrt einen Wahrheitswert um.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Nicht“** (auch bekannt als logische Negation) kehrt den Wahrheitswert einer Bedingung um. \s
                Das Ergebnis ist **wahr** (true), wenn die ursprüngliche Bedingung **falsch** ist. \s
                Ist die ursprüngliche Bedingung **wahr**, ergibt der Operator **„Nicht“** den Wert **falsch**.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob eine Person **keinen gültigen Ausweis** hat.
                
                Mit dem Operator **„Nicht“** wird diese Logik so formuliert: \s
                `NICHT Hat gültigen Ausweis`
                
                **Ergebnis:**
                - Wenn die Person **keinen gültigen Ausweis** hat, ist das Ergebnis **wahr**.
                - Wenn die Person **einen gültigen Ausweis** hat, ist das Ergebnis **falsch**.
                
                # Wahrheitswerte für den „Nicht“-Operator
                - **NICHT wahr** → **falsch**
                - **NICHT falsch** → **wahr**
                
                # Wann verwenden Sie den Operator „Nicht“?
                Verwenden Sie **„Nicht“**, wenn Sie prüfen möchten, ob eine Bedingung **nicht erfüllt** ist. \s
                Dies ist besonders hilfreich, wenn Sie Ausschlusskriterien festlegen möchten, z. B. um Bedingungen umzukehren oder auszudrücken, dass etwas „nicht wahr“ ist.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.Boolean,
                        "Wert",
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
        var arg = castToBoolean(args[0]);

        return new NoCodeResult(NoCodeDataType.Boolean, !arg);
    }
}