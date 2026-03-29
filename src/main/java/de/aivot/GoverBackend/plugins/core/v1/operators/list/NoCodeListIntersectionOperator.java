package de.aivot.GoverBackend.plugins.core.v1.operators.list;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;
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
        return "Schnittmenge zweier Listen";
    }

    @Override
    public String getAbstract() {
        return "Ermittelt die gemeinsamen Werte zweier Listen.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Schnittmenge zweier Listen“** ermittelt alle Werte, die in beiden Listen vorkommen. \s
                Das Ergebnis ist eine neue Liste mit den gemeinsamen Werten in der Reihenfolge ihres ersten Auftretens in der ersten Liste. \s
                Jeder gemeinsame Wert wird dabei nur einmal zurückgegeben.
                
                # Anwendungsbeispiel:
                Angenommen, Sie haben zwei Listen mit Interessen: `["Sport", "Musik", "Reisen"]` und `["Lesen", "Musik", "Reisen"]`. \s
                Mit dem Operator **„Schnittmenge zweier Listen“** können Sie die gemeinsamen Einträge ermitteln:
                
                ```text
                Schnittmenge zweier Listen (["Sport", "Musik", "Reisen"], ["Lesen", "Musik", "Reisen"])
                ```
                
                **Ergebnis:** \s
                Die zurückgegebene Liste enthält: `["Musik", "Reisen"]`.
                
                **Weitere Beispiele:**
                - `[1, 2, 3]` und `[2, 3, 4]` → **Ergebnis:** `[2, 3]` \s
                - `["A", "A", "B"]` und `["A"]` → **Ergebnis:** `["A"]` \s
                - `[]` und `["A"]` → **Ergebnis:** `[]`
                
                # Wann verwenden Sie den Operator „Schnittmenge zweier Listen“?
                Verwenden Sie **„Schnittmenge zweier Listen“**, wenn Sie:
                - Gemeinsamkeiten zwischen zwei Listen identifizieren möchten. \s
                - Prüfen möchten, welche Werte in zwei Datenquellen gleichzeitig vorkommen. \s
                - Listen auf überlappende Werte reduzieren möchten, um sie weiterzuverarbeiten.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.List,
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste 1",
                                "Die erste Liste, deren gemeinsame Werte ermittelt werden sollen."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste 2",
                                "Die zweite Liste, mit der die Schnittmenge gebildet werden soll."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "bestimme die Schnittmenge von „#0“ und „#1“";
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

        var castedRight = castToTypeOfReference(left, right);
        if (Objects.equals(left, castedRight)) {
            return true;
        }

        var castedLeft = castToTypeOfReference(right, left);
        return Objects.equals(castedLeft, right);
    }
}
