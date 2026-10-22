package de.aivot.prosuna.backend.plugins.core.v1.operators.phone;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.prosuna.backend.nocode.models.*;
import de.aivot.prosuna.backend.utils.PhoneNumberUtils;
import jakarta.annotation.Nonnull;
import java.util.Locale;

public class NoCodePhoneNumberFormatOperator extends NoCodeOperator {
    @Override @Nonnull public String getIdentifier() { return "phone-number-format"; }
    @Override @Nonnull public String getLabel() { return "Telefonnummer formatieren"; }
    @Override @Nonnull public String getAbstract() { return "Formatiert eine plausible Telefonnummer für die internationale oder nationale Darstellung."; }
    @Override @Nonnull public String getDescription() {
        return "Formatiert Telefonnummern mit Ländervorwahl, zum Beispiel aus einem Telefonnummer-Feld. "
                + "International wird +4930123456 als +49 30 123456 dargestellt, national als 030 123456. "
                + "Standard ist die internationale Darstellung. Nationale Nummern sind für Anrufe innerhalb des jeweiligen Landes gedacht. "
                + "Leere oder nicht plausible Nummern sowie separate Nebenstellenzusätze ergeben null. Die gespeicherte Telefonnummer bleibt unverändert.";
    }
    @Override @Nonnull public String[] getTags() { return new String[]{"Telefonnummer", "Formatierung", "Telefon", "Text"}; }
    @Override @Nonnull public String getHumanReadableTemplate() { return "formatierte Telefonnummer von „#0“"; }
    @Override @Nonnull public NoCodeSignatur[] getSignatures() {
        var number = new NoCodeParameter(NoCodeDataType.String, "Telefonnummer", "Die Telefonnummer mit internationaler Ländervorwahl.");
        return NoCodeSignatur.of(
                NoCodeSignatur.of(NoCodeDataType.String, number),
                NoCodeSignatur.of(NoCodeDataType.String, number,
                        new NoCodeParameter(NoCodeDataType.String, "Darstellung", "Standard: international.",
                                new NoCodeParameterOption("International", "international"), new NoCodeParameterOption("National", "national"))));
    }
    @Override @Nonnull
    public NoCodeResult performEvaluation(@Nonnull DerivedRuntimeElementData data, @Nonnull Object... args) throws NoCodeException {
        if (args.length < 1 || args.length > 2) throw new NoCodeWrongArgumentCountException(1, args.length);
        var format = args.length == 1 ? "" : castToString(args[1]).trim().toLowerCase(Locale.ROOT);
        var national = switch (format) {
            case "", "international" -> false;
            case "national" -> true;
            default -> throw new NoCodeException("Bitte wählen Sie international oder national als Darstellung aus.");
        };
        return new NoCodeResult(PhoneNumberUtils.formatPhoneNumber(castToString(args[0]), national));
    }
}
