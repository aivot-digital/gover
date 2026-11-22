package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import jakarta.annotation.Nullable;

public class NoCodeIfOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "if";
    }

    @Override
    public String getLabel() {
        return "Wenn, dann, sonst";
    }

    @Override
    public String getAbstract() {
        return "Wählt abhängig von einer Bedingung einen von zwei Werten aus.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Berechnen Sie beispielsweise den Betrag pro Eintrag, wenn Einträge vorliegen. Bei einer Anzahl von null können Sie stattdessen einen Hinweis ausgeben.

                # Welche Werte werden benötigt?
                - Als **Bedingung** wählen Sie „Wahr“ oder „Falsch“ aus der festen Auswahl. Über „Andere Wertquelle verwenden …“ können Sie eine Wertquelle oder Funktion nutzen.
                - Für **Ergebnis bei Wahr** und **Ergebnis bei Falsch** können Sie einen Text frei eingeben, einen Wert aus einer Wertquelle übernehmen oder eine Funktion verwenden.

                # Welches Ergebnis erhalten Sie?
                Ist die Bedingung wahr, erhalten Sie den Wert aus „Ergebnis bei Wahr“. Ist sie falsch, erhalten Sie den Wert aus „Ergebnis bei Falsch“. Die Funktion übernimmt den ausgewählten Wert mit seinem Datentyp unverändert.

                # Beispiel im No-Code-Editor
                1. Wählen Sie **„Wenn, dann, sonst“**. Verwenden Sie für die **„Bedingung“** die Funktion **„Größer als“**. Wählen Sie bei „Zahl“ die Variable „Anzahl“ und geben Sie bei „Vergleichszahl“ `0` ein.
                2. Wählen Sie für **„Ergebnis bei Wahr“** die Funktion **„Dividieren“**. Übernehmen Sie bei „Ausgangszahl“ die Variable „Gesamtbetrag“ und bei „Teiler 1“ die Variable „Anzahl“.
                3. Wählen Sie für **„Ergebnis bei Falsch“** die freie Texteingabe und geben Sie `Keine Einträge` ein.

                Aufgeklappt sehen Sie bei „wenn“ die Funktion „Größer als“, bei „dann“ die Funktion „Dividieren“ und bei „sonst“ den Text „Keine Einträge“. Zugeklappt und in der Vorschau lesen Sie `wenn („Anzahl“ ist größer als 0), dann („Gesamtbetrag“ geteilt durch „Anzahl“), sonst „Keine Einträge“`.

                Bei einem Gesamtbetrag von `24` und einer Anzahl von `3` erhalten Sie die Zahl `8`. Bei einer Anzahl von `0` erhalten Sie den Text „Keine Einträge“. Die Division wird nur bei einer Anzahl größer als null ausgeführt.

                # Was ist zu beachten?
                Die Funktion wertet zuerst die Bedingung aus. Liefert sie einen Wert mit einem anderen Datentyp, wird er für diese Entscheidung in einen Wahrheitswert umgewandelt. Anschließend wertet die Funktion nur den gewählten Ergebniszweig aus. Im Testlauf steht am übersprungenen Zweig „Nicht ausgewertet“ mit der Begründung.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Runtime,
                        new NoCodeParameter(
                                NoCodeDataType.Boolean,
                                "Bedingung",
                                "Die Bedingung, die das Ergebnis bestimmt.",
                                new NoCodeParameterOption("Wahr", "true"),
                                new NoCodeParameterOption("Falsch", "false")
                        ).withSentence("wenn"),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Ergebnis bei Wahr",
                                "Der Wert, der bei einer wahren Bedingung übernommen wird."
                        ).withSentence("dann"),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Ergebnis bei Falsch",
                                "Der Wert, der bei einer falschen Bedingung übernommen wird."
                        ).withSentence("sonst")
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        return new NoCodeResult(args[selectedResultIndex(args[0])]);
    }

    public int selectedResultIndex(@Nullable Object condition) {
        return castToBoolean(condition) ? 1 : 2;
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "wenn #0, dann #1, sonst #2";
    }
}
