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
        return """
                # Wofür verwenden Sie diese Funktion?
                Geben Sie eine Telefonnummer mit landesüblichen Trennzeichen aus, zum Beispiel als lesbare Kontaktnummer in einer Nachricht.

                # Welche Werte werden benötigt?
                - **Telefonnummer:** Eine Telefonnummer mit internationaler Ländervorwahl. Sie können sie frei eingeben, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Vorgegebene Auswahlwerte gibt es nicht.
                - **Darstellung:** In der Variante mit dieser zusätzlichen Eingabe wählen Sie **„International“** oder **„National“**. Zusätzliche eigene Einträge sind in dieser Auswahl nicht möglich. Die Darstellung kann auch aus einer anderen Wertquelle oder einer Funktion stammen.

                Ohne die zusätzliche Eingabe wird die internationale Darstellung verwendet.

                # Welches Ergebnis erhalten Sie?
                Einen Text mit der formatierten Telefonnummer. Bei einer fehlenden, leeren oder nicht plausiblen Nummer erhalten Sie keinen Wert (**null**). Die ursprüngliche Telefonnummer bleibt unverändert.

                # Beispiel im No-Code-Editor
                1. Wählen Sie für einen Textwert **„Telefonnummer formatieren“**.
                2. Wählen Sie als **„Telefonnummer“** ein Telefonnummer-Feld als andere Wertquelle oder legen Sie **+4930123456** selbst fest.
                3. Verwenden Sie das Ergebnis beispielsweise als Kontaktnummer in einem berechneten Text.

                Ohne zusätzliche Darstellung erhalten Sie **„+49 30 123456“**. Zugeklappt lesen Sie: **„formatierte Telefonnummer von „+4930123456““**. Mit der Darstellung **„National“** lautet das Ergebnis **„030 123456“**.

                # Was ist zu beachten?
                Die nationale Darstellung ist für die Verwendung innerhalb des jeweiligen Landes gedacht. Auch dafür muss die Eingabe zunächst die internationale Ländervorwahl enthalten.

                Die Funktion verwendet die Plausibilitätsprüfung und bestätigt weder die tatsächliche Vergabe noch die Erreichbarkeit. Separate Nebenstellenzusätze werden abgelehnt. Ein leerer Wert für **„Darstellung“** verwendet **„International“**.
                """;
    }
    @Override @Nonnull public String[] getTags() { return new String[]{"Telefonnummer", "Formatierung", "Telefon", "Text"}; }
    @Override @Nonnull public String getHumanReadableTemplate() { return "formatierte Telefonnummer von #0"; }
    @Override @Nonnull public NoCodeSignatur[] getSignatures() {
        var number = new NoCodeParameter(NoCodeDataType.String, "Telefonnummer", "Die Telefonnummer mit internationaler Ländervorwahl.").withSentence("für die Telefonnummer");
        return NoCodeSignatur.of(
                NoCodeSignatur.of(NoCodeDataType.String, number),
                NoCodeSignatur.of(NoCodeDataType.String, number,
                        new NoCodeParameter(NoCodeDataType.String, "Darstellung", "Wählen Sie die internationale Darstellung mit Ländervorwahl oder die nationale Darstellung für das jeweilige Land.",
                                new NoCodeParameterOption("International", "international"), new NoCodeParameterOption("National", "national")).withSentence("in der Darstellung")));
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

    @Nonnull
    @Override
    public String getSentenceLead() {
        return "ergibt den Text";
    }
}
