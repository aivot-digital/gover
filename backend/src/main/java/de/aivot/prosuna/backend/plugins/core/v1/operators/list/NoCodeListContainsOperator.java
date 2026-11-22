package de.aivot.prosuna.backend.plugins.core.v1.operators.list;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class NoCodeListContainsOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-contains";
    }

    @Override
    public String getLabel() {
        return "Liste enthält Wert";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob ein gesuchter Wert in einer Liste vorkommt.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Prüfen Sie, ob ein bestimmter Wert zu einer Liste gehört, beispielsweise ob eine ausgewählte Stadt unterstützt wird.

                # Welche Werte werden benötigt?
                **Liste:** Die Liste, in der gesucht wird. **Gesuchter Wert:** Der einzelne Wert, den Sie suchen.

                Die Liste können Sie selbst als JSON-Liste eingeben, beispielsweise `[1, 2, 3]`. Es gibt keine vorgegebenen Auswahlwerte. Alternativ verwenden Sie eine andere Wertquelle oder eine Funktion, die eine Liste liefert. Den gesuchten Wert können Sie frei eingeben oder aus einer anderen Wertquelle beziehungsweise Funktion übernehmen.

                # Welches Ergebnis erhalten Sie?
                **Wahr**, wenn der gesuchte Wert mindestens einmal vorkommt; andernfalls **Falsch**.

                # Beispiel im No-Code-Editor
                Wählen Sie in einer Bedingung **„Liste enthält Wert“**. Verwenden Sie für **Liste** die Variable „Unterstützte Städte“ mit `["Berlin", "Hamburg"]` und geben Sie als **Gesuchter Wert** `Hamburg` ein.

                Zugeklappt und in der Vorschau lesen Sie: `„Unterstützte Städte“ enthält „Hamburg“`. Das Ergebnis ist **Wahr**.

                # Was ist zu beachten?
                Eine leere Liste ergibt **Falsch**. Zahlen und Texte mit derselben Zahl können als gleich gelten, etwa `2` und `"2"`. Verwenden Sie möglichst einheitliche Werttypen. Ein fehlender Wert kann nur zu einem ebenfalls fehlenden Listeneintrag passen.
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
                                NoCodeDataType.Runtime,
                                "Liste",
                                "Eine Liste von Werten, in der gesucht werden soll."
                        ).withSentence("wenn die Liste"),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Gesuchter Wert",
                                "Der Wert, nach dem in der Liste gesucht werden soll."
                        ).withSentence("den Wert", "enthält")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 enthält #1";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var list = listOperandOrEmpty(args[0]);
        var value = args[1];

        if (value == null) {
            return new NoCodeResult(list.stream().anyMatch(Objects::isNull));
        }

        for (Object item : list) {
            if (item == null) {
                continue;
            }

            if (isEqualToReference(value, item)) {
                return new NoCodeResult(true);
            }
        }

        return new NoCodeResult(false);
    }
}
