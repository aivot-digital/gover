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

public class NoCodePhoneNumberNormalizeOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "phone-number-normalize";
    }

    @Override
    public String getLabel() {
        return "Telefonnummer normalisieren";
    }

    @Override
    public String getAbstract() {
        return "Normalisiert eine plausible Telefonnummer mit Ländervorwahl in das E.164-Format.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Telefonnummer normalisieren“** wandelt eine Telefonnummer mit internationaler Ländervorwahl in das E.164-Format um.

                Die Normalisierung erfolgt, wenn die Telefonnummer grundsätzlich plausibel ist. Der Operator prüft nicht streng, ob die Nummer zu einem gültigen Nummernbereich gehört. Wenn Sie das vor der Normalisierung sicherstellen möchten, kombinieren Sie diesen Operator mit **„Telefonnummer ist gültig“**.

                Bei einer nicht plausiblen Telefonnummer gibt der Operator **null** zurück.

                # Beispiele:
                - **+49 30 123456** → **+4930123456**
                - **+49 1234** → **+491234**
                - **030 123456** → **null**
                """;
    }

    @Override
    public String[] getTags() {
        return new String[]{"Telefonnummer", "Normalisierung", "Text", "E.164"};
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.String,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Telefonnummer",
                                "Die zu normalisierende Telefonnummer mit Ländervorwahl."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "normalisierte Telefonnummer von „#0“";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length != 1) {
            throw new NoCodeWrongArgumentCountException(1, args.length);
        }

        return new NoCodeResult(PhoneNumberUtils.normalizePossiblePhoneNumberToE164(castToString(args[0])));
    }
}
