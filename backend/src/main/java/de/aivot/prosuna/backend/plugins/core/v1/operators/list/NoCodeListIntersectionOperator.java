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
import java.util.List;
import java.util.Objects;

public class NoCodeListIntersectionOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-intersection";
    }

    @Override
    public String getLabel() {
        return "Gemeinsame Werte zweier Listen";
    }

    @Override
    public String getAbstract() {
        return "Ermittelt die Werte, die in beiden Listen vorkommen, jeweils einmal.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Ermitteln Sie die gemeinsamen Werte zweier Listen, beispielsweise zulässige Kategorien aus einer Auswahl.

                # Welche Werte werden benötigt?
                **Erste Liste** und **Zweite Liste:** Die beiden Listen, die zusammen betrachtet werden. Sie können jede Liste selbst als JSON-Liste eingeben, beispielsweise `["A", "B"]`, oder eine andere Wertquelle beziehungsweise Funktion verwenden. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                Eine neue Liste mit den Werten, die in beiden Listen vorkommen. Jeder gemeinsame Wert steht nur einmal im Ergebnis.

                # Beispiel im No-Code-Editor
                Wählen Sie **„Gemeinsame Werte zweier Listen“**. Verwenden Sie für **Erste Liste** die Variable „Ausgewählte Kategorien“ mit `["B", "A", "B"]` und für **Zweite Liste** die Variable „Zulässige Kategorien“ mit `["A", "B", "C"]`.

                Zugeklappt und in der Vorschau lesen Sie: `gemeinsame Werte von „Ausgewählte Kategorien“ und „Zulässige Kategorien“`. Das Ergebnis ist `["B", "A"]`. Mit **„Anzahl der Listeneinträge“** können Sie anschließend die gemeinsamen Werte zählen.

                # Was ist zu beachten?
                Die Reihenfolge und die Darstellung der Werte folgen der ersten Liste. Ist eine der Listen leer, ist auch das Ergebnis leer. Zahlen und Texte mit derselben Zahl können als gleich gelten, etwa `2` und `"2"`. Verwenden Sie möglichst einheitliche Werttypen. Ein fehlender Wert kann nur zu einem ebenfalls fehlenden Listeneintrag passen.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt die gemeinsamen Werte";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.List,
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Erste Liste",
                                "Die erste Liste, deren gemeinsame Werte ermittelt werden sollen."
                        ).withSentence("von"),
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Zweite Liste",
                                "Die zweite Liste, mit der die Schnittmenge gebildet werden soll."
                        ).withSentence("und")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "gemeinsame Werte von #0 und #1";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var left = castToList(args[0]);
        var right = castToList(args[1]);

        var result = new ArrayList<>();

        for (var item : left) {
            if (containsMatchingValue(right, item) && !containsMatchingValue(result, item)) {
                result.add(item);
            }
        }

        return new NoCodeResult(result);
    }

    private boolean containsMatchingValue(List<Object> list, @Nullable Object value) {
        if (value == null) {
            return list.stream().anyMatch(Objects::isNull);
        }

        for (var item : list) {
            if (isEquivalent(value, item)) {
                return true;
            }
        }

        return false;
    }

    private boolean isEquivalent(@Nullable Object left, @Nullable Object right) {
        if (left == null || right == null) {
            return left == null && right == null;
        }

        return isEqualToReference(left, right) || isEqualToReference(right, left);
    }
}
