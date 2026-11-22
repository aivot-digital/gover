package de.aivot.prosuna.backend.plugins.core.v1.operators.list;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.util.ArrayList;

public class NoCodeListConcatOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-concat";
    }

    @Override
    public String getLabel() {
        return "Listen zusammenfügen";
    }

    @Override
    public String getAbstract() {
        return "Hängt die Einträge einer zweiten Liste an die erste Liste an.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Führen Sie zwei Listen zusammen, beispielsweise bereits erfasste und zusätzlich ausgewählte Kategorien.

                # Welche Werte werden benötigt?
                **Erste Liste** und **Zweite Liste:** Die beiden Listen, die zusammen betrachtet werden. Sie können jede Liste selbst als JSON-Liste eingeben, beispielsweise `["A", "B"]`, oder eine andere Wertquelle beziehungsweise Funktion verwenden. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                Eine neue Liste: zuerst alle Einträge der ersten Liste, danach alle Einträge der zweiten Liste.

                # Beispiel im No-Code-Editor
                Wählen Sie **„Listen zusammenfügen“**. Verwenden Sie für **Erste Liste** die Variable „Bisherige Kategorien“ mit `["A", "B"]` und für **Zweite Liste** die Variable „Weitere Kategorien“ mit `["B", "C"]`.

                Zugeklappt und in der Vorschau lesen Sie: `„Bisherige Kategorien“ gefolgt von „Weitere Kategorien“`. Das Ergebnis ist `["A", "B", "B", "C"]`. Diese Liste kann anschließend als Eingabe einer weiteren Listenfunktion dienen.

                # Was ist zu beachten?
                Die Reihenfolge bleibt erhalten. Doppelte Einträge bleiben enthalten. Die beiden ursprünglichen Listen werden nicht verändert.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt die Liste";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.List,
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Erste Liste",
                                "Die erste Liste, die zusammengefügt werden soll."
                        ).withSentence("aus"),
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Zweite Liste",
                                "Die zweite Liste, die zusammengefügt werden soll."
                        ).withSentence("und")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 gefolgt von #1";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var list1 = castToList(args[0]);
        var list2 = castToList(args[1]);

        var result = new ArrayList<>(list1);
        result.addAll(list2);

        return new NoCodeResult(result);
    }
}
