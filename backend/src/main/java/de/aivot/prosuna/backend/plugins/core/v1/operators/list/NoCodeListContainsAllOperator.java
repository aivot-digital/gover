package de.aivot.prosuna.backend.plugins.core.v1.operators.list;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nonnull;
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
        return "Liste enthält alle Werte";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob jeder gesuchte Wert in einer Liste vorkommt.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Prüfen Sie, ob eine Liste alle benötigten Werte enthält, beispielsweise alle erforderlichen Kategorien.

                # Welche Werte werden benötigt?
                **Liste:** Die Liste, in der gesucht wird. **Gesuchte Werte:** Eine zweite Liste mit allen Werten, die enthalten sein sollen. Beide Listen können Sie selbst als JSON-Liste eingeben oder aus einer anderen Wertquelle beziehungsweise Funktion übernehmen. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                **Wahr**, wenn jeder gesuchte Wert vorkommt; andernfalls **Falsch**. Die erste Liste darf zusätzliche Werte enthalten.

                # Beispiel im No-Code-Editor
                Wählen Sie in einer Bedingung **„Liste enthält alle Werte“**. Verwenden Sie für **Liste** die Variable „Ausgewählte Kategorien“ mit `["A", "B", "C"]` und für **Gesuchte Werte** die Variable „Erforderliche Kategorien“ mit `["A", "C"]`.

                Zugeklappt und in der Vorschau lesen Sie: `„Ausgewählte Kategorien“ enthält alle Werte aus „Erforderliche Kategorien“`. Das Ergebnis ist **Wahr**.

                # Was ist zu beachten?
                Die Reihenfolge ist unerheblich. Eine leere Liste gesuchter Werte ergibt **Wahr**. Mehrfach gesuchte gleiche Werte müssen nicht mehrfach in der ersten Liste vorkommen. Zahlen und Texte mit derselben Zahl können als gleich gelten, etwa `2` und `"2"`. Verwenden Sie möglichst einheitliche Werttypen. Ein fehlender Wert kann nur zu einem ebenfalls fehlenden Listeneintrag passen.
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
                        ).withSentence("wenn die Liste"),
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Gesuchte Werte",
                                "Die Werte, die alle in der Liste enthalten sein müssen."
                        ).withSentence("alle Werte aus", "enthält")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 enthält alle Werte aus #1";
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

    @Nonnull
    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }
}
