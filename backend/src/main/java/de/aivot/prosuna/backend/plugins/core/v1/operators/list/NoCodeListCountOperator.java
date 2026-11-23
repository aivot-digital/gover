package de.aivot.prosuna.backend.plugins.core.v1.operators.list;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

public class NoCodeListCountOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-count";
    }

    @Override
    public String getLabel() {
        return "Vorkommen in Liste zählen";
    }

    @Override
    public String getAbstract() {
        return "Zählt, wie oft ein gesuchter Wert in einer Liste vorkommt.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Ermitteln Sie, wie oft ein bestimmter Wert in einer Liste vorkommt, beispielsweise die Anzahl gleicher Antworten.

                # Welche Werte werden benötigt?
                **Liste:** Die Liste, in der gezählt wird. **Gesuchter Wert:** Der einzelne Wert, dessen Vorkommen Sie zählen möchten.

                Die Liste können Sie selbst als JSON-Liste eingeben, beispielsweise `[1, 2, 3]`. Es gibt keine vorgegebenen Auswahlwerte. Alternativ verwenden Sie eine andere Wertquelle oder eine Funktion, die eine Liste liefert. Den gesuchten Wert können Sie frei eingeben oder aus einer anderen Wertquelle beziehungsweise Funktion übernehmen.

                # Welches Ergebnis erhalten Sie?
                Eine ganze Zahl mit der Anzahl passender Einträge. Gibt es keine passenden Einträge, ist das Ergebnis `0`.

                # Beispiel im No-Code-Editor
                Wählen Sie **„Vorkommen in Liste zählen“**. Verwenden Sie für **Liste** die Variable „Antworten“ mit `["Ja", "Nein", "Ja"]` und geben Sie als **Gesuchter Wert** `Ja` ein.

                Zugeklappt und in der Vorschau lesen Sie: `Anzahl der Vorkommen von „Ja“ in „Antworten“`. Das Ergebnis ist `2`. Sie können das Ergebnis mit einer Zahl vergleichen, beispielsweise mit **„Größer als“** und `1`.

                # Was ist zu beachten?
                Eine leere Liste ergibt `0`. Fehlt die Liste oder ist sie leerer Text oder `null`, gilt sie als leer. Werte, die keine Liste sind, etwa Zahlen, Objekte oder Text, der keine JSON-Liste ist, führen zu einer Fehlermeldung. Jeder passende Eintrag wird gezählt, auch wenn derselbe Wert mehrfach vorkommt. Eine Zahl gilt als gleich mit einem Text derselben Zahl, etwa `2` mit `"2"` oder `"2,0"`. Verwenden Sie möglichst einheitliche Werttypen. Ein fehlender Wert kann nur zu einem ebenfalls fehlenden Listeneintrag passen.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt, wie oft";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Number,
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste",
                                "Die Liste, in der gezählt werden soll."
                        ).withSentence("in der Liste"),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Gesuchter Wert",
                                "Der Wert, dessen Vorkommen in der Liste gezählt werden soll."
                        ).withSentence("der Wert", "vorkommt")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "Anzahl der Vorkommen von #1 in #0";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var list = listOperandOrEmpty(args[0]);
        var value = args[1];

        var count = 0;
        for (var item : list) {
            if (matchesValue(item, value)) {
                count++;
            }
        }

        return new NoCodeResult(count);
    }

    private boolean matchesValue(@Nullable Object item, @Nullable Object value) {
        if (value == null) {
            return item == null;
        }

        if (item == null) {
            return false;
        }

        return isEqualToReference(value, item);
    }
}
