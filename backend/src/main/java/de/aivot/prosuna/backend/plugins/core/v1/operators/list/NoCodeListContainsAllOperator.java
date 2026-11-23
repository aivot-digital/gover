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

public class NoCodeListContainsAllOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-contains-all";
    }

    @Override
    public String getLabel() {
        return "Liste beinhaltet alle";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob eine Liste alle Werte einer zweiten Liste enthält.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Liste beinhaltet alle“** prüft, ob jeder Wert der zweiten Liste auch in der ersten Liste vorkommt. \s
                Die Reihenfolge spielt keine Rolle. Die erste Liste darf weitere Werte enthalten.

                # Beispiele:
                - `["A", "B", "C"]` beinhaltet alle von `["A", "C"]` → **wahr**
                - `["A", "B"]` beinhaltet alle von `["A", "D"]` → **falsch**
                - `["A"]` beinhaltet alle von `[]` → **wahr**

                Um zu prüfen, ob mindestens ein Wert vorkommt, verwenden Sie **„Listen überschneiden sich“**.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste",
                                "Die Liste, in der gesucht werden soll."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Gesuchte Werte",
                                "Die Werte, die alle in der Liste enthalten sein müssen."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "„#0“ beinhaltet alle Werte aus „#1“";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var list = castToList(args[0]);
        var searched = castToList(args[1]);

        for (var value : searched) {
            if (!containsMatchingValue(list, value)) {
                return new NoCodeResult(false);
            }
        }

        return new NoCodeResult(true);
    }

    // Same equivalence as list-overlaps, so both operators agree on which values match.
    private boolean containsMatchingValue(List<Object> list, @Nullable Object value) {
        if (value == null) {
            return list.stream().anyMatch(Objects::isNull);
        }

        return list.stream().anyMatch(item -> item != null && (isEqualToReference(value, item) || isEqualToReference(item, value)));
    }
}
