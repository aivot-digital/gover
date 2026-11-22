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
        return "Liste beinhaltet";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob ein bestimmter Wert in einer Liste enthalten ist.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Die Funktion **„Liste beinhaltet“** prüft, ob ein bestimmter Wert in einer Liste enthalten ist. \s
                Das Ergebnis ist **wahr** (true), wenn der Wert in der Liste vorkommt. \s
                Ist der Wert nicht in der Liste enthalten, ergibt die Funktion den Wert **falsch**.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob eine ausgewählte Stadt in einer Liste von unterstützten Städten enthalten ist.
                
                Mit der Funktion **„Liste beinhaltet“** wird diese Logik so formuliert: \s
                `Liste beinhaltet Ausgewählte Stadt`
                
                Beispielwerte: \s
                - **Liste:** ["Berlin", "Hamburg", "München"] \s
                - **Ausgewählte Stadt:** "Hamburg"
                
                **Ergebnis:**
                - **Bedingung wahr:** Wenn die ausgewählte Stadt „Hamburg“ ist, ist das Ergebnis **wahr**. \s
                - **Bedingung falsch:** Wenn die ausgewählte Stadt „Köln“ ist, ist das Ergebnis **falsch**.
                
                # Wahrheitswerte der Funktion „Liste beinhaltet“
                - **Liste ["Apfel", "Banane", "Orange"] beinhaltet "Banane"** → **wahr** \s
                - **Liste ["Apfel", "Banane", "Orange"] beinhaltet "Traube"** → **falsch** \s
                - **Liste [] beinhaltet "Apfel"** → **falsch** \s
                - **Liste ["1", "2", "3"] beinhaltet "2"** → **wahr**
                
                # Wann verwenden Sie die Funktion „Liste beinhaltet“?
                Verwenden Sie **„Liste beinhaltet“**, um zu prüfen, ob ein Wert Teil einer festgelegten Liste ist. \s
                Diese Funktion ist besonders hilfreich bei Validierungen, z. B. um zu überprüfen, ob eine Benutzereingabe in einer Liste zulässiger Werte enthalten ist, oder bei Filterfunktionen, um Zugehörigkeiten zu prüfen.
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
                        ).withSentence("wenn"),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Wert",
                                "Der Wert, nach dem in der Liste gesucht werden soll."
                        ).withSentence("den Wert", "enthält")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "„#0“ beinhaltet „#1“";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var list = castToList(args[0]);
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
