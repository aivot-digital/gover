package de.aivot.prosuna.backend.plugins.core.v1.operators.list;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeNumberConstraints;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

public class NoCodeListGetOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-get";
    }

    @Override
    public String getLabel() {
        return "Eintrag aus Liste";
    }

    @Override
    public String getAbstract() {
        return "Liest einen einzelnen Eintrag an einer angegebenen Position aus einer Liste.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Lesen Sie einen bestimmten Eintrag aus einer Liste, beispielsweise die erste oder letzte ausgewählte Stadt.

                # Welche Werte werden benötigt?

                - **Liste:** Die Liste mit den Einträgen.
                - **Position (ab 0):** Die Stelle des gesuchten Eintrags. `0` bezeichnet den ersten Eintrag, `1` den zweiten. Mit `-1` erhalten Sie den letzten und mit `-2` den vorletzten Eintrag.

                Die Liste können Sie selbst als JSON-Liste eingeben, beispielsweise `[1, 2, 3]`. Es gibt keine vorgegebenen Auswahlwerte. Alternativ verwenden Sie eine andere Wertquelle oder eine Funktion, die eine Liste liefert. Die Position geben Sie frei als ganze Zahl ein oder übernehmen sie aus einer anderen Wertquelle beziehungsweise Funktion.

                # Welches Ergebnis erhalten Sie?

                Der Eintrag an der angegebenen Position. Liegt die Position außerhalb der Liste, erhalten Sie keinen Wert.

                # Beispiel im No-Code-Editor

                1. Wählen Sie **„Eintrag aus Liste“**.
                2. Verwenden Sie für **„Liste“** die Variable **„Städte“** mit `["Berlin", "Hamburg", "München"]` und geben Sie unter **„Position (ab 0)“** `2` ein.

                Zugeklappt und in der Vorschau lesen Sie: `Eintrag an Position 2 aus „Städte“ (Zählung ab 0)`. Das Ergebnis ist der Text `München`. Verwenden Sie die Funktion beispielsweise als Text einer weiteren Textfunktion.

                # Was ist zu beachten?

                Die Zählung beginnt bei `0`, nicht bei `1`. Negative Positionen zählen vom Ende der Liste aus. Eine leere Liste liefert keinen Eintrag. Fehlt die Liste oder ist sie leerer Text oder `null`, gilt sie als leer. Werte, die keine Liste sind, etwa Zahlen, Objekte oder Text, der keine JSON-Liste ist, führen zu einer Fehlermeldung.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt den Eintrag";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Runtime,
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste",
                                "Die Liste, aus der ein Element abgerufen werden soll"
                        ).withSentence("aus"),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Position (ab 0)",
                                "Ganze Zahl: 0 ist der erste Eintrag, 1 der zweite; -1 ist der letzte Eintrag."
                        ).withSentence("an Position", "(Zählung ab 0)")
                                .withNumberConstraints(new NoCodeNumberConstraints(true, (double) Integer.MIN_VALUE, (double) Integer.MAX_VALUE))
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "Eintrag an Position #1 aus #0 (Zählung ab 0)";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var list = listOperandOrEmpty(args[0]);
        var position = requireInteger(args[1], "Die Position muss eine ganze Zahl zwischen -2147483648 und 2147483647 sein.");
        var index = position < 0 ? list.size() + position : position;
        return new NoCodeResult(index >= 0 && index < list.size() ? list.get(index) : null);
    }
}
