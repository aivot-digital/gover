package de.aivot.prosuna.backend.plugins.core.v1.operators.list;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

public class NoCodeListLengthOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-length";
    }

    @Override
    public String getLabel() {
        return "Anzahl der Listeneinträge";
    }

    @Override
    public String getAbstract() {
        return "Zählt alle Einträge einer Liste.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Zählen Sie die Einträge einer Liste, beispielsweise die ausgewählten Städte oder die Zeilen einer Tabelle.

                # Welche Werte werden benötigt?

                - **Liste:** Die Liste, deren Einträge Sie zählen möchten.

                Die Liste können Sie selbst als JSON-Liste eingeben, beispielsweise `[1, 2, 3]`. Es gibt keine vorgegebenen Auswahlwerte. Alternativ verwenden Sie eine andere Wertquelle oder eine Funktion, die eine Liste liefert.

                # Welches Ergebnis erhalten Sie?

                Eine ganze Zahl mit der Anzahl der Einträge. Eine leere Liste ergibt `0`.

                # Beispiel im No-Code-Editor

                1. Wählen Sie **„Anzahl der Listeneinträge“**.
                2. Verwenden Sie für **„Liste“** die Variable **„Städte“** mit `["Berlin", "Hamburg", "München"]`.

                Zugeklappt und in der Vorschau lesen Sie: `Anzahl der Einträge in „Städte“`. Das Ergebnis ist `3`. Verwenden Sie die Funktion beispielsweise als ersten Wert von **„Größer als“** und vergleichen Sie mit `0`.

                # Was ist zu beachten?

                Jeder Eintrag zählt mit, auch doppelte Einträge und Einträge ohne Wert. Diese Funktion zählt alle Einträge; **„Vorkommen in Liste zählen“** zählt nur die passenden Einträge eines gesuchten Wertes. Fehlt die Liste oder ist sie leerer Text oder `null`, gilt sie als leer. Werte, die keine Liste sind, etwa Zahlen, Objekte oder Text, der keine JSON-Liste ist, führen zu einer Fehlermeldung.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt die Anzahl der Einträge";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Number,
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste",
                                "Die Liste, deren Länge ermittelt werden soll."
                        ).withSentence("in")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "Anzahl der Einträge in #0";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        return new NoCodeResult(listOperandOrEmpty(args[0]).size());
    }
}
