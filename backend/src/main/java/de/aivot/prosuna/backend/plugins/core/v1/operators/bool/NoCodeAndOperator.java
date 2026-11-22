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
                Prüfen Sie beispielsweise, ob Einträge vorliegen und der Betrag pro Eintrag über einem Grenzwert liegt.

                # Welche Werte werden benötigt?
                Sie benötigen „Bedingung 1“ und „Bedingung 2“. Für jede Bedingung können Sie „Wahr“ oder „Falsch“ aus der festen Auswahl wählen. Über „Andere Wertquelle verwenden …“ können Sie eine Wertquelle oder eine Funktion verwenden.

                # Welches Ergebnis erhalten Sie?
                Sie erhalten „Wahr“, wenn beide Bedingungen wahr sind. Ist mindestens eine Bedingung falsch, erhalten Sie „Falsch“.

                # Beispiel im No-Code-Editor
                1. Wählen Sie **„Und“**. Verwenden Sie für **„Bedingung 1“** die Funktion **„Größer als“**. Wählen Sie bei „Zahl“ die Variable „Anzahl“ und geben Sie bei „Vergleichszahl“ `0` ein.
                2. Verwenden Sie auch für **„Bedingung 2“** die Funktion **„Größer als“**. Wählen Sie bei „Zahl“ die Funktion **„Dividieren“**. Übernehmen Sie als „Ausgangszahl“ die Variable „Gesamtbetrag“ und als „Teiler 1“ die Variable „Anzahl“.
                3. Geben Sie als „Vergleichszahl“ der zweiten Bedingung `10` ein.

                Zugeklappt und in der Vorschau lesen Sie `(„Anzahl“ ist größer als 0) und ((„Gesamtbetrag“ geteilt durch „Anzahl“) ist größer als 10)`.

                Bei einer Anzahl von `0` erhalten Sie „Falsch“. Die Division wird übersprungen. Bei einer Anzahl von `3` und einem Gesamtbetrag von `36` liegt der Betrag pro Eintrag bei `12`. Beide Bedingungen sind wahr und Sie erhalten „Wahr“.

                # Was ist zu beachten?
                Die Funktion prüft die Bedingungen von oben nach unten. Ist „Bedingung 1“ falsch, steht das Ergebnis fest und „Bedingung 2“ wird übersprungen. Im Testlauf sehen Sie dort „Nicht ausgewertet“ mit der Begründung. Ist „Bedingung 1“ wahr, wird auch „Bedingung 2“ ausgewertet.

                Für weitere Bedingungen können Sie „Und“ verschachteln. Verwenden Sie möglichst Wertquellen, die Wahrheitswerte liefern. Andere Werttypen werden bei der Auswertung in Wahrheitswerte umgewandelt. Der Text „Nein“ gilt dabei als wahr.
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
