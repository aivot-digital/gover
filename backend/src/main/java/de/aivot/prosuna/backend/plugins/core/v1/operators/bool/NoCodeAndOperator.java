package de.aivot.prosuna.backend.plugins.core.v1.operators.bool;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import jakarta.annotation.Nullable;

public class NoCodeAndOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "and";
    }

    @Override
    public String getLabel() {
        return "Und";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob beide Bedingungen erfüllt sind.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Verknüpfen Sie Bedingungen, die gemeinsam erfüllt sein müssen, etwa eine Altersgrenze und eine bestätigte Einwilligung.

                # Welche Werte werden benötigt?
                Zwei Bedingungen. Für selbst festgelegte Bedingungen stehen „Wahr“ und „Falsch“ zur Auswahl. Eigene Auswahlwerte sind nicht möglich. Über „Andere Wertquelle verwenden …“ können Sie Bedingungen aus Wertquellen oder Funktionen übernehmen.

                # Welches Ergebnis erhalten Sie?
                Ein Wahrheitswert: „Wahr“, wenn beide Bedingungen wahr sind. Ist mindestens eine Bedingung falsch, lautet das Ergebnis „Falsch“.

                # Beispiel im No-Code-Editor
                1. Wählen Sie „Und“ als Funktion für eine Bedingung.
                2. Wählen Sie für „Bedingung 1“ über „Andere Wertquelle verwenden …“ die Wertquelle „Einwilligung“, die einen Wahrheitswert liefert.
                3. Wählen Sie für „Bedingung 2“ die Wertquelle „Ausweis geprüft“.

                Sind beide Bedingungen wahr, ergibt der Ausdruck „Wahr“. Ist nur eine wahr, ergibt er „Falsch“.

                # Was ist zu beachten?
                Für weitere Bedingungen können Sie „Und“ verschachteln. Verwenden Sie Wertquellen, die Wahrheitswerte liefern. Andere Werttypen werden bei der Auswertung in Wahrheitswerte umgewandelt; ein Text wie „Nein“ wird dabei nicht automatisch zu „Falsch“. Verschachtelte Funktionen in allen Bedingungen werden vor der Verknüpfung ausgewertet und müssen auswertbar sein.
                """;
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
                                NoCodeDataType.Boolean,
                                "Bedingung 1",
                                "Eine Bedingung, die einen Wahrheitswert liefert.",
                                new NoCodeParameterOption("Wahr", "true"),
                                new NoCodeParameterOption("Falsch", "false")
                        ).withSentence("wenn", "wahr ist"),
                        new NoCodeParameter(
                                NoCodeDataType.Boolean,
                                "Bedingung 2",
                                "Eine weitere Bedingung, die einen Wahrheitswert liefert.",
                                new NoCodeParameterOption("Wahr", "true"),
                                new NoCodeParameterOption("Falsch", "false")
                        ).withSentence("und", "wahr ist")
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        for (Object o : args) {
            var arg = castToBoolean(o);
            if (!arg) {
                return new NoCodeResult(false);
            }
        }
        return new NoCodeResult(true);
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 und #1";
    }
}
