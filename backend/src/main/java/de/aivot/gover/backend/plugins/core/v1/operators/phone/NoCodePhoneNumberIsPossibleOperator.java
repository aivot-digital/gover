package de.aivot.gover.backend.plugins.core.v1.operators.phone;

import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.nocode.enums.NoCodeDataType;
import de.aivot.gover.backend.nocode.exceptions.NoCodeException;
import de.aivot.gover.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.gover.backend.nocode.models.NoCodeOperator;
import de.aivot.gover.backend.nocode.models.NoCodeParameter;
import de.aivot.gover.backend.nocode.models.NoCodeResult;
import de.aivot.gover.backend.nocode.models.NoCodeSignatur;
import de.aivot.gover.backend.utils.PhoneNumberUtils;
import jakarta.annotation.Nullable;

public class NoCodePhoneNumberIsPossibleOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "phone-number-is-possible";
    }

    @Override
    public String getLabel() {
        return "Telefonnummer ist plausibel";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob eine Telefonnummer mit Ländervorwahl grundsätzlich plausibel ist.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Telefonnummer ist plausibel“** prüft, ob eine Telefonnummer mit internationaler Ländervorwahl grundsätzlich plausibel ist.

                Die Prüfung ist bewusst weniger streng als **„Telefonnummer ist gültig“**. Sie prüft vor allem, ob die Telefonnummer für das jeweilige Land eine mögliche Länge und Struktur hat. Neue oder seltene Nummernbereiche werden dadurch weniger wahrscheinlich abgelehnt.

                Die Telefonnummer muss mit einem `+` und der Ländervorwahl beginnen. Nebenstellen oder Durchwahlen in Form von Erweiterungen werden nicht akzeptiert.

                # Beispiele:
                - **+49 30 123456** → **wahr**
                - **+49 1234** → **wahr**
                - **030 123456** → **falsch**
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
        return "„#0“ ist eine plausible Telefonnummer";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length != 1) {
            throw new NoCodeWrongArgumentCountException(1, args.length);
        }

        return new NoCodeResult(PhoneNumberUtils.isPossiblePhoneNumber(castToString(args[0])));
    }
}
