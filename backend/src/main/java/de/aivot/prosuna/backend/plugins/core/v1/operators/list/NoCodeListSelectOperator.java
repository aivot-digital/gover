package de.aivot.prosuna.backend.plugins.core.v1.operators.list;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

public class NoCodeListSelectOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-select";
    }

    @Override
    public String getLabel() {
        return "Feldwerte aus Liste";
    }

    @Override
    public String getAbstract() {
        return "Liest dasselbe Feld aus jedem Eintrag einer Liste aus.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Lesen Sie eine Spalte einer Tabelle oder ein bestimmtes Feld aus allen Einträgen einer Liste aus, beispielsweise die Beträge aller Positionen.

                # Welche Werte werden benötigt?
                **Liste:** Eine Tabelle oder Liste mit Einträgen, die Felder enthalten. Sie können eine andere Wertquelle oder Funktion verwenden oder die Liste selbst als JSON eingeben, beispielsweise `[{"betrag": 100}, {"betrag": 50}]`.

                **Feldname:** Der technische Name des Feldes, beispielsweise `betrag`. Diesen Namen geben Sie frei ein oder übernehmen ihn aus einer anderen Wertquelle beziehungsweise Funktion. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                Eine neue Liste mit einem Feldwert je ursprünglichem Eintrag. Fehlt das Feld in einem Eintrag, steht an dieser Stelle ein fehlender Wert.

                # Beispiel im No-Code-Editor
                Wählen Sie **„Feldwerte aus Liste“**. Verwenden Sie für **Liste** die Variable „Positionen“ mit `[{"betrag": 100}, {"betrag": 50}]` und geben Sie als **Feldname** `betrag` ein.

                Zugeklappt und in der Vorschau lesen Sie: `Werte des Feldes „betrag“ aus „Positionen“`. Das Ergebnis ist `[100, 50]`. Setzen Sie diese Funktion als **Liste** in **„Summe einer Liste“** ein, um die Gesamtsumme `150` zu berechnen.

                # Was ist zu beachten?
                Verwenden Sie den technischen Feldnamen mit passender Groß- und Kleinschreibung. Es wird nur ein direktes Feld gelesen; ein Name wie `person.name` ist kein Zugriff auf ein verschachteltes Feld. Die Reihenfolge und Anzahl der Einträge bleiben erhalten.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt die Feldwerte";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.List,
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste",
                                "Die Liste oder Tabelle, aus der die Werte extrahiert werden sollen."
                        ).withSentence("aus"),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Feldname",
                                "Technischer Name des direkten Feldes, beispielsweise betrag; beachten Sie Groß- und Kleinschreibung."
                        ).withSentence("für das Feld")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "Werte des Feldes #1 aus #0";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var list = listOperandOrEmpty(args[0]);
        var field = castToString(args[1]);

        var result = list
                .stream()
                .map(this::castToMap)
                .map(map -> map == null ? null : map.get(field))
                .toList();

        return new NoCodeResult(result);
    }
}
