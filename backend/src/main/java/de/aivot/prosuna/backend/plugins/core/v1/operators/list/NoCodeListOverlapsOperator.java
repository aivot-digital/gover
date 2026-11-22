package de.aivot.prosuna.backend.plugins.core.v1.operators.list;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public class NoCodeListOverlapsOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-overlaps";
    }

    @Override
    public String getLabel() {
        return "Listen überschneiden sich";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob zwei Listen mindestens einen gemeinsamen Wert haben.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Prüfen Sie, ob zwei Listen mindestens einen gemeinsamen Wert haben, beispielsweise ob eine Auswahl eine zulässige Kategorie enthält.

                # Welche Werte werden benötigt?
                **Erste Liste** und **Zweite Liste:** Die beiden Listen, die zusammen betrachtet werden. Sie können jede Liste selbst als JSON-Liste eingeben, beispielsweise `["A", "B"]`, oder eine andere Wertquelle beziehungsweise Funktion verwenden. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                **Wahr**, sobald ein Wert in beiden Listen vorkommt; andernfalls **Falsch**.

                # Beispiel im No-Code-Editor
                Wählen Sie in einer Bedingung **„Listen überschneiden sich“**. Verwenden Sie für **Erste Liste** die Variable „Ausgewählte Kategorien“ mit `["X", "B"]` und für **Zweite Liste** die Variable „Zulässige Kategorien“ mit `["A", "B", "C"]`.

                Zugeklappt und in der Vorschau lesen Sie: `„Ausgewählte Kategorien“ und „Zulässige Kategorien“ haben mindestens einen gemeinsamen Wert`. Das Ergebnis ist **Wahr**.

                # Was ist zu beachten?
                Eine leere Liste ergibt **Falsch**. Um alle gesuchten Werte zu prüfen, verwenden Sie **„Liste enthält alle Werte“**. Zahlen und Texte mit derselben Zahl können als gleich gelten, etwa `2` und `"2"`. Verwenden Sie möglichst einheitliche Werttypen. Ein fehlender Wert kann nur zu einem ebenfalls fehlenden Listeneintrag passen.
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
                                NoCodeDataType.List,
                                "Erste Liste",
                                "Die erste Liste, die auf gemeinsame Werte geprüft werden soll."
                        ).withSentence("wenn"),
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Zweite Liste",
                                "Die zweite Liste, mit der die Überschneidung geprüft werden soll."
                        ).withSentence("und", "mindestens einen gemeinsamen Wert haben")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 und #1 haben mindestens einen gemeinsamen Wert";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var left = castToList(args[0]);
        var right = castToList(args[1]);

        for (var item : left) {
            if (containsMatchingValue(right, item)) {
                return new NoCodeResult(true);
            }
        }

        return new NoCodeResult(false);
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
