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

public class NoCodePhoneNumberNormalizeOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "phone-number-normalize";
    }

    @Override
    public String getLabel() {
        return "Telefonnummer vereinheitlichen";
    }

    @Override
    public String getAbstract() {
        return "Gibt eine plausible Telefonnummer mit Ländervorwahl ohne Trennzeichen aus.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Bringen Sie eine Telefonnummer in eine einheitliche Schreibweise, zum Beispiel bevor Sie Kontaktdaten weitergeben oder Telefonnummern miteinander vergleichen.

                # Welche Werte werden benötigt?

                - **Telefonnummer:** Eine Telefonnummer mit internationaler Ländervorwahl, zum Beispiel `+49 30 123456`. Sie können sie frei eingeben, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Vorgegebene Auswahlwerte gibt es nicht.

                # Welches Ergebnis erhalten Sie?

                Einen Text mit `+`, Ländervorwahl und Ziffern ohne Trennzeichen, zum Beispiel `+4930123456`. Diese Schreibweise heißt E.164. Bei einer fehlenden, leeren oder nicht plausiblen Nummer erhalten Sie keinen Wert (`null`).

                # Beispiel im No-Code-Editor

                1. Wählen Sie für einen Textwert **„Telefonnummer vereinheitlichen“**.
                2. Wählen Sie als **„Telefonnummer“** ein Telefonnummer-Feld als andere Wertquelle oder legen Sie `+49 30 123456` selbst fest.
                3. Verwenden Sie das Ergebnis beispielsweise als einen der beiden Textwerte in einem Vergleich mit **„Ist gleich“**.

                Das Ergebnis ist `+4930123456`. Zugeklappt lesen Sie: `vereinheitlichte Telefonnummer von „+49 30 123456“`.

                # Was ist zu beachten?

                Die Funktion verwendet die weniger strenge Plausibilitätsprüfung. Daher wird beispielsweise auch `+49 1234` in `+491234` umgewandelt. Wenn die Nummer zusätzlich zum bekannten Nummernplan passen muss, prüfen Sie sie vorher mit **„Telefonnummer ist gültig“**.

                Separate Nebenstellenzusätze werden abgelehnt. Die Funktion prüft nicht, ob eine Nummer vergeben oder erreichbar ist, und verändert den ursprünglichen Eingabewert nicht.
                """;
    }

    @Override
    public String[] getTags() {
        return new String[]{"Telefonnummer", "Normalisierung", "Text", "E.164"};
    }

    @Override
    public String getSentenceLead() {
        return "ergibt die vereinheitlichte Telefonnummer";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.String,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Telefonnummer",
                                "Die Telefonnummer mit + und internationaler Ländervorwahl, zum Beispiel +49 30 123456."
                        ).withSentence("von")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "vereinheitlichte Telefonnummer von #0";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length != 1) {
            throw new NoCodeWrongArgumentCountException(1, args.length);
        }

        return new NoCodeResult(PhoneNumberUtils.normalizePossiblePhoneNumberToE164(castToString(args[0])));
    }
}
