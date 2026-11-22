package de.aivot.prosuna.backend.plugins.core.v1.operators.bool;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import jakarta.annotation.Nullable;

public class NoCodeOrOperator extends NoCodeOperator {
    public static final String OPERATOR_ID = "or";

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
                Verknüpfen Sie Bedingungen, von denen mindestens eine erfüllt sein muss, etwa zwei alternative Voraussetzungen.

                # Welche Werte werden benötigt?
                Zwei bis zehn Bedingungen; wählen Sie die passende Anzahl an Eingaben. Für selbst festgelegte Bedingungen stehen „Wahr“ und „Falsch“ zur Auswahl. Eigene Auswahlwerte sind nicht möglich. Über „Andere Wertquelle verwenden …“ können Sie Bedingungen aus Wertquellen oder Funktionen übernehmen.

                # Welches Ergebnis erhalten Sie?
                Ein Wahrheitswert: „Wahr“, wenn mindestens eine Bedingung wahr ist. Das gilt auch, wenn mehrere Bedingungen wahr sind. Nur wenn alle Bedingungen falsch sind, lautet das Ergebnis „Falsch“.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Oder“ als Funktion für eine Bedingung.
                2. Wählen Sie für „Bedingung 1“ über „Andere Wertquelle verwenden …“ die Wertquelle „Einwilligung“, die einen Wahrheitswert liefert.
                3. Wählen Sie für „Bedingung 2“ die Wertquelle „Andere Zustimmung liegt vor“.

                Ist eine der beiden Bedingungen wahr, ergibt der Ausdruck „Wahr“. Sind beide falsch, ergibt er „Falsch“.

                # Was ist zu beachten?
                Bei drei oder mehr Bedingungen werden alle ausgewählten Eingaben berücksichtigt. Verwenden Sie Wertquellen, die Wahrheitswerte liefern. Andere Werttypen werden bei der Auswertung in Wahrheitswerte umgewandelt; ein Text wie „Nein“ wird dabei nicht automatisch zu „Falsch“. Verschachtelte Funktionen in allen Bedingungen werden vor der Verknüpfung ausgewertet und müssen auswertbar sein.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        var signatures = new NoCodeSignatur[9];

        for (int i = 0; i < 9; i++) {
            NoCodeParameter[] parameters = new NoCodeParameter[i + 2];
            for (int j = 0; j < i + 2; j++) {
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
