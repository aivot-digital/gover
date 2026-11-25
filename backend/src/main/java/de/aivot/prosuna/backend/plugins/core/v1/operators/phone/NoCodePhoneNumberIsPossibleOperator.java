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
                # Wofür verwenden Sie diese Funktion?
                Prüfen Sie, ob eine Telefonnummer grundsätzlich möglich ist, zum Beispiel bevor Sie eine eingegebene Kontaktnummer weiterverwenden.

                # Welche Werte werden benötigt?
                **Telefonnummer:** Eine Telefonnummer mit internationaler Ländervorwahl, zum Beispiel **+49 30 123456**. Sie können sie frei eingeben, aus einer anderen Wertquelle übernehmen oder mit einer Funktion ermitteln. Vorgegebene Auswahlwerte gibt es nicht.

                # Welches Ergebnis erhalten Sie?
                **Wahr**, wenn die Telefonnummer für das jeweilige Land grundsätzlich möglich ist. Andernfalls erhalten Sie **Falsch**, auch bei einer fehlenden oder leeren Angabe.

                # Beispiel im No-Code-Editor
                1. Wählen Sie für eine Bedingung **„Telefonnummer ist plausibel“**.
                2. Wählen Sie als **„Telefonnummer“** ein Telefonnummer-Feld als andere Wertquelle oder legen Sie **+49 30 123456** selbst fest.
                3. Verwenden Sie das Ergebnis beispielsweise als Bedingung für die weitere Verarbeitung der Kontaktnummer.

                Für **+49 30 123456** ist das Ergebnis **Wahr**. Zugeklappt lesen Sie: **„„+49 30 123456“ ist eine plausible Telefonnummer“**.

                # Was ist zu beachten?
                Die Prüfung berücksichtigt insbesondere die mögliche Länge einer Telefonnummer im jeweiligen Land. Sie ist weniger streng als **„Telefonnummer ist gültig“** und akzeptiert beispielsweise auch **+49 1234**.

                Die Nummer muss mit **+** und der Ländervorwahl beginnen. Leerzeichen, Klammern, Schrägstriche, Bindestriche und Punkte sind als Schreibweisen möglich; separate Nebenstellenzusätze werden abgelehnt. Die Prüfung sagt nicht aus, ob die Nummer vergeben oder erreichbar ist.
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
                        ).withSentence("wenn", "eine plausible Telefonnummer ist")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 ist eine plausible Telefonnummer";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length != 1) {
            throw new NoCodeWrongArgumentCountException(1, args.length);
        }

        return new NoCodeResult(PhoneNumberUtils.isPossiblePhoneNumber(castToString(args[0])));
    }
}
