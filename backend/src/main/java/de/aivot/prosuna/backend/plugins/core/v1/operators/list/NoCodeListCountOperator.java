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
        return "Zählt, wie oft ein bestimmter Wert in einer Liste vorkommt.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Vorkommen in Liste zählen“** zählt, wie oft ein bestimmter Wert in einer Liste vorkommt. \s
                Das Ergebnis ist eine Zahl.
                
                # Anwendungsbeispiel:
                Angenommen, Sie möchten zählen, wie oft eine ausgewählte Antwort in einer Liste enthalten ist.
                
                Mit dem Operator **„Vorkommen in Liste zählen“** wird diese Logik so formuliert: \s
                `Vorkommen in Liste zählen Antworten "Ja"`
                
                Beispielwerte: \s
                - **Liste:** ["Ja", "Nein", "Ja"] \s
                - **Wert:** "Ja"
                
                **Ergebnis:** 2
                
                # Wann verwenden Sie den Operator „Vorkommen in Liste zählen“?
                Verwenden Sie **„Vorkommen in Liste zählen“**, wenn Sie ermitteln möchten, wie häufig ein Wert in einer Liste enthalten ist.
                """;
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
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Wert",
                                "Der Wert, dessen Vorkommen in der Liste gezählt werden soll."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "zähle, wie oft „#1“ in „#0“ vorkommt";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var list = castToList(args[0]);
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

        var castedItem = castToTypeOfReference(value, item);
        return Objects.equals(castedItem, value);
    }
}
