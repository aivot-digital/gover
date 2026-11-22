package de.aivot.prosuna.backend.plugins.core.v1.operators.bool;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import jakarta.annotation.Nullable;

public class NoCodeOrOperator extends NoCodeOperator {
    public static final String OPERATOR_ID = "or";
    private static final int MINIMUM_CONDITIONS = 2;
    private static final int MAXIMUM_CONDITIONS = 10;

    public static boolean supportsConditionCount(int count) {
        return count >= MINIMUM_CONDITIONS && count <= MAXIMUM_CONDITIONS;
    }

    @Override
    public String getIdentifier() {
        return OPERATOR_ID;
    }

    @Override
    public String getLabel() {
        return "Oder";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob mindestens eine der Bedingungen erfüllt ist.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Zeigen Sie beispielsweise einen Hinweis an, wenn die Anzahl der Einträge null oder negativ ist oder der Betrag pro Eintrag über einem Grenzwert liegt.

                # Welche Werte werden benötigt?
                Wählen Sie im Editor zwei bis zehn Bedingungen. Für jede Bedingung können Sie „Wahr“ oder „Falsch“ aus der festen Auswahl wählen. Über „Andere Wertquelle verwenden …“ können Sie eine Wertquelle oder eine Funktion verwenden.

                # Welches Ergebnis erhalten Sie?
                Sobald die erste wahre Bedingung gefunden ist, erhalten Sie „Wahr“. Sind alle Bedingungen falsch, erhalten Sie „Falsch“.

                # Beispiel im No-Code-Editor
                1. Wählen Sie **„Oder“** mit zwei Bedingungen. Verwenden Sie für **„Bedingung 1“** die Funktion **„Kleiner als oder gleich“**. Wählen Sie bei „Zahl“ die Variable „Anzahl“ und geben Sie bei „Vergleichszahl“ `0` ein.
                2. Verwenden Sie für **„Bedingung 2“** die Funktion **„Größer als“**. Wählen Sie bei „Zahl“ die Funktion **„Dividieren“**. Übernehmen Sie als „Ausgangszahl“ die Variable „Gesamtbetrag“ und als „Teiler 1“ die Variable „Anzahl“.
                3. Geben Sie als „Vergleichszahl“ der zweiten Bedingung `10` ein.

                Zugeklappt und in der Vorschau lesen Sie `(„Anzahl“ ist kleiner als oder gleich 0) oder ((„Gesamtbetrag“ geteilt durch „Anzahl“) ist größer als 10)`.

                Bei einer Anzahl von `0` erhalten Sie „Wahr“. Die Division wird übersprungen. Im Testlauf steht bei „Bedingung 2“ der Status „Nicht ausgewertet“ mit der Begründung „Bedingung 1 ist wahr.“

                Bei einer Anzahl von `3` und einem Gesamtbetrag von `36` wird die zweite Bedingung geprüft. Der Betrag pro Eintrag liegt mit `12` über dem Grenzwert von `10`. Sie erhalten „Wahr“.

                # Was ist zu beachten?
                Die Funktion prüft die Bedingungen von oben nach unten. Ist eine Bedingung wahr, überspringt sie alle folgenden Bedingungen. Im Testlauf steht an den dabei übersprungenen Bedingungen, welche Bedingung wahr war.

                Verwenden Sie möglichst Wertquellen, die Wahrheitswerte liefern. Andere Werttypen werden bei der Auswertung in Wahrheitswerte umgewandelt. Der Text „Nein“ gilt dabei als wahr.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        var signatures = new NoCodeSignatur[MAXIMUM_CONDITIONS - MINIMUM_CONDITIONS + 1];

        for (int i = 0; i < signatures.length; i++) {
            NoCodeParameter[] parameters = new NoCodeParameter[i + MINIMUM_CONDITIONS];
            for (int j = 0; j < parameters.length; j++) {
                parameters[j] = new NoCodeParameter(
                        NoCodeDataType.Boolean,
                        "Bedingung " + (j + 1),
                        "Eine Bedingung, die einen Wahrheitswert liefert.",
                        new NoCodeParameterOption("Wahr", "true"),
                        new NoCodeParameterOption("Falsch", "false")
                ).withSentence(j == 0 ? "wenn" : "oder", "wahr ist");
            }
            signatures[i] = NoCodeSignatur.of(
                    NoCodeDataType.Boolean,
                    parameters
            );
        }

        return signatures;
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        for (Object arg : args) {
            var boolArg = castToBoolean(arg);
            if (boolArg) {
                return new NoCodeResult(true);
            }
        }
        return new NoCodeResult(false);
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 oder #1";
    }
}
