package de.aivot.GoverBackend.plugins.core.v1.operators.common;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class NoCodeEqualsOperator extends NoCodeOperator {
    public static final String OPERATOR_ID = "equals";

    @Override
    public String getIdentifier() {
        return OPERATOR_ID;
    }

    @Override
    public String getLabel() {
        return "Ist Gleich";
    }

    @Override
    public String getAbstract() {
        return "Vergleicht zwei Werte miteinander, um festzustellen, ob sie gleich sind.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Ist Gleich“** vergleicht zwei Werte miteinander, um festzustellen, ob sie gleich sind. \s
                Das Ergebnis ist **wahr** (true), wenn die beiden Werte exakt gleich sind. \s
                Sind die beiden Werte unterschiedlich, ergibt der Operator **„Ist Gleich“** den Wert **falsch**.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob eine eingegebene Postleitzahl der benötigten Postleitzahl entspricht.
                
                Mit dem Operator **„Ist Gleich“** wird diese Logik so formuliert: \s
                `Eingabe Postleitzahl IST GLEICH Erwartete Postleitzahl`
                
                **Ergebnis:**
                - Wenn die eingegebene Postleitzahl exakt der erwarteten Postleitzahl entspricht, ist das Ergebnis **wahr**.
                - Wenn die eingegebene Postleitzahl von der erwarteten Postleitzahl abweicht, ist das Ergebnis **falsch**.
                
                # Wahrheitswerte für den „Ist Gleich“-Operator
                - **10 IST GLEICH 10** → **wahr** \s
                - **10 IST GLEICH 20** → **falsch** \s
                - **"Hallo" IST GLEICH "Hallo"** → **wahr** \s
                - **"Hallo" IST GLEICH "Welt"** → **falsch**
                
                # Wann verwenden Sie den Operator „Ist Gleich“?
                Verwenden Sie **„Ist Gleich“**, wenn Sie prüfen möchten, ob zwei Werte exakt übereinstimmen. \s
                Dieser Operator ist besonders hilfreich bei Validierungen, z. B. um sicherzustellen, dass Benutzereingaben mit den erwarteten Werten übereinstimmen.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Wert 1",
                                "Der erste Wert, der mit dem zweiten Wert verglichen werden soll."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Wert 2",
                                "Der zweite Wert, der mit dem ersten Wert verglichen werden soll."
                        )
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
        var arg0 = args[0];
        var arg1 = args[1];

        if (arg0 == null && arg1 == null) {
            return new NoCodeResult(true);
        }

        if (arg0 == null || arg1 == null) {
            return new NoCodeResult(false);
        }

        var castedArg1 = castToTypeOfReference(arg0, arg1);
        return new NoCodeResult(Objects.equals(arg0, castedArg1));
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "„#0“ ist gleich „#1“";
    }
}
