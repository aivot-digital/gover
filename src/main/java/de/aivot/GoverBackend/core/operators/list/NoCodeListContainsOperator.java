package de.aivot.GoverBackend.core.operators.list;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;

import java.util.Objects;

public class NoCodeListContainsOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-contains";
    }

    @Override
    public String getLabel() {
        return "Liste Beinhaltet";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob ein bestimmter Wert in einer Liste enthalten ist.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Liste Beinhaltet“** prüft, ob ein bestimmter Wert in einer Liste enthalten ist. \s
                Das Ergebnis ist **wahr** (true), wenn der Wert in der Liste vorkommt. \s
                Ist der Wert nicht in der Liste enthalten, ergibt der Operator den Wert **falsch**.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob eine ausgewählte Stadt in einer Liste von unterstützten Städten enthalten ist.
                
                Mit dem Operator **„Liste Beinhaltet“** wird diese Logik so formuliert: \s
                `Liste Beinhaltet Ausgewählte Stadt`
                
                Beispielwerte: \s
                - **Liste:** ["Berlin", "Hamburg", "München"] \s
                - **Ausgewählte Stadt:** "Hamburg"
                
                **Ergebnis:**
                - **Bedingung wahr:** Wenn die ausgewählte Stadt „Hamburg“ ist, ist das Ergebnis **wahr**. \s
                - **Bedingung falsch:** Wenn die ausgewählte Stadt „Köln“ ist, ist das Ergebnis **falsch**.
                
                # Wahrheitswerte für den „Liste Beinhaltet“-Operator
                - **Liste ["Apfel", "Banane", "Orange"] Beinhaltet "Banane"** → **wahr** \s
                - **Liste ["Apfel", "Banane", "Orange"] Beinhaltet "Traube"** → **falsch** \s
                - **Liste [] Beinhaltet "Apfel"** → **falsch** \s
                - **Liste ["1", "2", "3"] Beinhaltet "2"** → **wahr**
                
                # Wann verwenden Sie den Operator „Liste Beinhaltet“?
                Verwenden Sie **„Liste Beinhaltet“**, um zu prüfen, ob ein Wert Teil einer festgelegten Liste ist. \s
                Dieser Operator ist besonders hilfreich bei Validierungen, z. B. um zu überprüfen, ob eine Benutzereingabe in einer Liste zulässiger Werte enthalten ist, oder bei Filterfunktionen, um Zugehörigkeiten zu prüfen.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Number,
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Liste",
                                "Eine Liste von Werten, in der gesucht werden soll."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Wert",
                                "Der Wert, nach dem in der Liste gesucht werden soll."
                        )
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
        var list = castToList(args[0]);
        var value = args[1];

        for (Object item : list) {
            var castedItem = castToTypeOfReference(value, item);
            if (Objects.equals(castedItem, value)) {
                return new NoCodeResult(true);
            }
        }

        return new NoCodeResult(false);
    }
}
