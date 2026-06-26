package de.aivot.GoverBackend.plugins.core.v1.operators.list;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;
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
        return "Prüft, ob zwei Listen mindestens einen gemeinsamen Wert enthalten.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Listen überschneiden sich“** prüft, ob zwei Listen mindestens einen gemeinsamen Wert enthalten. \s
                Das Ergebnis ist **wahr**, sobald sich ein Wert in beiden Listen befindet. \s
                Gibt es keinen gemeinsamen Eintrag, ist das Ergebnis **falsch**.
                
                # Anwendungsbeispiel:
                Angenommen, Sie haben eine Liste mit erlaubten Kategorien und eine zweite Liste mit den vom Nutzer ausgewählten Kategorien. \s
                Mit dem Operator **„Listen überschneiden sich“** können Sie prüfen, ob mindestens eine Auswahl zulässig ist:
                
                ```text
                Listen überschneiden sich (["A", "B", "C"], ["X", "B", "Y"])
                ```
                
                **Ergebnis:** \s
                Da beide Listen den Wert `"B"` enthalten, ist das Ergebnis `wahr`.
                
                **Weitere Beispiele:**
                - `[1, 2, 3]` und `[4, 5, 6]` → **Ergebnis:** `falsch` \s
                - `[1, 2, 3]` und `["3", 4]` → **Ergebnis:** `wahr` \s
                - `[]` und `["A"]` → **Ergebnis:** `falsch`
                
                # Wann verwenden Sie den Operator „Listen überschneiden sich“?
                Verwenden Sie **„Listen überschneiden sich“**, wenn Sie:
                - Prüfen möchten, ob zwei Listen gemeinsame Werte haben. \s
                - Regeln oder Filter auslösen möchten, sobald eine Überschneidung vorliegt. \s
                - Schnell feststellen möchten, ob zwischen zwei Datenmengen eine Gemeinsamkeit besteht.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste 1",
                                "Die erste Liste, die auf gemeinsame Werte geprüft werden soll."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste 2",
                                "Die zweite Liste, mit der die Überschneidung geprüft werden soll."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "prüfe, ob sich „#0“ und „#1“ überschneiden";
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

        var castedRight = castToTypeOfReference(left, right);
        if (Objects.equals(left, castedRight)) {
            return true;
        }

        var castedLeft = castToTypeOfReference(right, left);
        return Objects.equals(castedLeft, right);
    }
}
