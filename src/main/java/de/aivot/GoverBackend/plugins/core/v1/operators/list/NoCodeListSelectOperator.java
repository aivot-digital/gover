package de.aivot.GoverBackend.plugins.core.v1.operators.list;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;

public class NoCodeListSelectOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-select";
    }

    @Override
    public String getLabel() {
        return "Werte aus Liste";
    }

    @Override
    public String getAbstract() {
        return "Extrahiert die Werte einer bestimmten Spalte (Feld) aus einer Tabelle oder einer Liste von Objekten.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Werte aus Liste“** dient dazu, die Werte einer bestimmten **Spalte** (Feld) aus einer Tabelle oder einer Liste von Objekten zu extrahieren. \s
                Das Ergebnis ist eine neue Liste, die nur die Werte des angegebenen Feldes enthält. \s
                Dieser Operator ist ideal, wenn Sie gezielt Daten aus einer bestimmten Spalte einer Tabelle benötigen, um diese weiterzuverarbeiten oder anzuzeigen.
                
                # Anwendungsbeispiel:
                Angenommen, Sie haben eine Tabelle mit Kundendaten, die Felder wie **„Name“**, **„Alter“** und **„Stadt“** enthält. \s
                Mit dem Operator **„Werte aus Liste“** können Sie beispielsweise die Spalte **„Name“** extrahieren, um nur die Namen der Kunden zu erhalten:
                
                ```text
                Werte aus Liste Kunden -> Spalte: "Name"
                ```
                
                **Ergebnis:** \s
                Die zurückgegebene Liste enthält die Namen der Kunden: `["Anna", "Ben", "Clara"]`.
                
                **Weitere Beispiele:**
                - Extraktion der Spalte **„Alter“** ergibt: `[25, 30, 22]` \s
                - Extraktion der Spalte **„Stadt“** ergibt: `["Berlin", "Hamburg", "München"]`
                
                # Wann verwenden Sie den Operator „Werte aus Liste“?
                Verwenden Sie **„Werte aus Liste“**, wenn Sie gezielt die Werte einer Spalte aus einer Tabelle oder Liste extrahieren möchten. \s
                Dieser Operator ist besonders nützlich in den folgenden Szenarien:
                
                - Sie möchten alle Werte aus einem bestimmten Feld (z. B. „Name“ oder „ID“) extrahieren, um sie weiterzuverarbeiten. \s
                - Sie benötigen die Daten einer Spalte, z. B. für Filter, Analysen oder Berichte. \s
                - Sie möchten eine Liste von Werten aus einer Tabelle erzeugen, z. B. um diese in Auswahlmenüs oder als Optionsfelder anzuzeigen.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Runtime,
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste",
                                "Die Liste oder Tabelle, aus der die Werte extrahiert werden sollen."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Feld",
                                "Das Feld (Spalte), dessen Werte extrahiert werden sollen."
                        )
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
        var list = castToList(args[0]);
        var field = castToString(args[1]);

        var result = list
                .stream()
                .map(this::castToMap)
                .map(map -> map.getOrDefault(field, null))
                .toList();

        return new NoCodeResult(result);
    }
}
