package de.aivot.GoverBackend.plugins.core.v1.operators.list;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

public class NoCodeListLengthOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-length";
    }

    @Override
    public String getLabel() {
        return "Listenlänge";
    }

    @Override
    public String getAbstract() {
        return "Gibt die Anzahl der Elemente in einer Liste zurück.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Listenlänge“** gibt die Anzahl der Elemente in einer Liste zurück. \s
                Das Ergebnis ist eine Ganzzahl, die die Anzahl der Elemente in der Liste darstellt.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten die Anzahl der Städte in einer Liste von Städten ermitteln.
                
                Mit dem Operator **„Listenlänge“** wird diese Logik so formuliert: \s
                `Listenlänge Liste`
                
                Beispielwerte: \s
                - **Liste:** ["Berlin", "Hamburg", "München"]
                
                **Ergebnis:**
                - **Rückgabewert:** 3
                
                # Wahrheitswerte für den „Listenlänge“-Operator
                - **Liste ["Apfel", "Banane", "Orange"]** → **3** \s
                - **Liste []** → **0** \s
                - **Liste ["1", "2", "3", "4"]** → **4**
                
                # Wann verwenden Sie den Operator „Listenlänge“?
                Verwenden Sie **„Listenlänge“**, um die Anzahl der Elemente in einer Liste zu ermitteln. \s
                Dieser Operator ist besonders hilfreich, wenn Sie die Größe einer Liste überprüfen oder verarbeiten möchten.
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
                                "Die Liste, deren Länge ermittelt werden soll."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "die Länge von „#0“";
    }

    @Override
    public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
        return new NoCodeResult(castToList(args[0]).size());
    }
}
