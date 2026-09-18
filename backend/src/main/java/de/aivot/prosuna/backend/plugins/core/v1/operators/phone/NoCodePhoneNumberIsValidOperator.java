package de.aivot.prosuna.backend.plugins.core.v1.operators.phone;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import de.aivot.prosuna.backend.utils.PhoneNumberUtils;
import jakarta.annotation.Nullable;

public class NoCodePhoneNumberIsValidOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "phone-number-is-valid";
    }

    @Override
    public String getLabel() {
        return "Telefonnummer ist gültig";
    }

    @Override
    public String getAbstract() {
        return "Prüft streng, ob eine Telefonnummer mit Ländervorwahl gültig ist.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Telefonnummer ist gültig“** prüft, ob eine Telefonnummer mit internationaler Ländervorwahl gültig ist.

                Die Telefonnummer muss mit einem `+` und der Ländervorwahl beginnen. Nebenstellen oder Durchwahlen in Form von Erweiterungen werden nicht akzeptiert.

                # Beispiele:
                - **+49 30 123456** → **wahr**
                - **+49 1234** → **falsch**
                - **030 123456** → **falsch**

                Verwenden Sie diesen Operator, wenn eine Telefonnummer fachlich streng validiert werden soll.
                """;
    }

    @Override
    public String[] getTags() {
        return new String[]{"Telefonnummer", "Validierung", "Text"};
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Telefonnummer",
                                "Die zu prüfende Telefonnummer mit Ländervorwahl."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "„#0“ ist eine gültige Telefonnummer";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length != 1) {
            throw new NoCodeWrongArgumentCountException(1, args.length);
        }

        return new NoCodeResult(PhoneNumberUtils.isValidPhoneNumber(castToString(args[0])));
    }
}
