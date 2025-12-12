package de.aivot.GoverBackend.plugins.corePlugin.components.operators.list;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;

public class NoCodeListGetOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-get";
    }

    @Override
    public String getLabel() {
        return "Listenzugriff";
    }

    @Override
    public String getAbstract() {
        return "Gibt ein Element einer Liste zurück.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Listenzugriff“** gibt ein Element einer Liste zurück. \s
                Das erste Element hat den Index 0. Sollte der Index nicht existieren, wird ein leerer Wert zurückgegeben. \s
                Bei einem negativen Index wird von hinten angefangen zu zählen.
                Der Index -1 entspricht dem letzten Element, -2 dem vorletzten Element usw.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten das dritte Element aus einer Liste von Städten abrufen.
                
                Mit dem Operator **„Listenzugriff“** wird diese Logik so formuliert: \s
                `Listenzugriff Liste Index`
                
                Beispielwerte: \s
                - **Liste:** ["Berlin", "Hamburg", "München"] \s
                - **Index:** 2
                
                **Ergebnis:**
                - **Rückgabewert:** "München"
                
                # Wahrheitswerte für den „Listenzugriff“-Operator
                - **Liste ["Apfel", "Banane", "Orange"] Index 1** → **"Banane"** \s
                - **Liste ["Apfel", "Banane", "Orange"] Index 3** → **null** \s
                - **Liste ["Apfel", "Banane", "Orange"] Index -1** → **"Orange"** \s
                - **Liste [] Index 0** → **null**
                
                # Wann verwenden Sie den Operator „Listenzugriff“?
                Verwenden Sie **„Listenzugriff“**, um ein bestimmtes Element aus einer Liste zu extrahieren. \s
                Dieser Operator ist besonders hilfreich, wenn Sie gezielt auf Elemente innerhalb einer Liste zugreifen möchten, z. B. um spezifische Daten zu extrahieren oder zu verarbeiten.
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
                                "Die Liste, aus der ein Element abgerufen werden soll"
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Index",
                                "Der Index des Elements, das abgerufen werden soll (0-basiert; negative Werte zählen von hinten)"
                        )
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
        var list = castToList(args[0]);
        var index = castToNumber(args[1]).intValue();

        if (index < 0) {
            list = list.reversed();
            index = Math.abs(index) - 1;
        }

        return new NoCodeResult(list.stream().skip(index).findFirst().orElse(null));
    }
}
