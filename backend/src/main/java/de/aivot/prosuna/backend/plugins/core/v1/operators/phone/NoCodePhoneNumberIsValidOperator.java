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
        return "Prüft eine Telefonnummer mit Ländervorwahl anhand des bekannten Nummernplans.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Prüfen Sie, ob eine Telefonnummer zum bekannten Nummernplan ihres Landes passt, zum Beispiel wenn Sie strengere Anforderungen als eine reine Plausibilitätsprüfung benötigen.

                # Welche Werte werden benötigt?

                - **Telefonnummer:** Eine Telefonnummer mit internationaler Ländervorwahl, zum Beispiel `+49 30 123456`. Sie können sie frei eingeben, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Vorgegebene Auswahlwerte gibt es nicht.

                # Welches Ergebnis erhalten Sie?

                `Wahr`, wenn die Telefonnummer die Prüfung des Nummernplans besteht. Andernfalls erhalten Sie `Falsch`, auch bei einer fehlenden oder leeren Angabe.

                # Beispiel im No-Code-Editor

                1. Wählen Sie für eine Bedingung **„Telefonnummer ist gültig“**.
                2. Wählen Sie als **„Telefonnummer“** ein Telefonnummer-Feld als andere Wertquelle oder legen Sie `+49 30 123456` selbst fest.
                3. Verwenden Sie das Ergebnis beispielsweise als Bedingung, bevor Sie die Nummer mit **„Telefonnummer vereinheitlichen“** weiterverarbeiten.

                Für `+49 30 123456` ist das Ergebnis `Wahr`; für `+49 1234` ist es `Falsch`. Zugeklappt lesen Sie: `„+49 30 123456“ ist eine gültige Telefonnummer`.

                # Was ist zu beachten?

                Die Prüfung ist strenger als **„Telefonnummer ist plausibel“**. Sie prüft den bekannten Nummernplan, aber weder die tatsächliche Vergabe noch die Erreichbarkeit der Nummer. Neue Nummernbereiche können deshalb zunächst abgelehnt werden.

                Die Nummer muss mit `+` und der Ländervorwahl beginnen. Übliche Trennzeichen sind möglich; separate Nebenstellenzusätze werden abgelehnt.
                """;
    }

    @Override
    public String[] getTags() {
        return new String[]{"Telefonnummer", "Validierung", "Text"};
    }

    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Telefonnummer",
                                "Die Telefonnummer mit + und internationaler Ländervorwahl, zum Beispiel +49 30 123456."
                        ).withSentence("wenn", "eine gültige Telefonnummer ist")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 ist eine gültige Telefonnummer";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length != 1) {
            throw new NoCodeWrongArgumentCountException(1, args.length);
        }

        return new NoCodeResult(PhoneNumberUtils.isValidPhoneNumber(castToString(args[0])));
    }
}
