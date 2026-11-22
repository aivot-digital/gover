package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import jakarta.annotation.Nullable;

public class NoCodeIsUndefinedOperator extends NoCodeIsDefinedOperator {
    @Override
    public String getIdentifier() {
        return "is-undefined";
    }

    @Override
    public String getLabel() {
        return "Ist leer";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob ein gegebener Wert leer ist.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Die Funktion **„Ist leer“** prüft, ob ein gegebener Wert **leer** ist. \s
                Das Ergebnis ist **wahr** (true), wenn der Wert leer, null oder undefiniert ist. \s
                Ist der Wert vorhanden (also nicht leer), ergibt die Funktion den Wert **falsch**.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob eine Person keine E-Mail-Adresse angegeben hat.
                
                Mit der Funktion **„Ist leer“** wird diese Logik so formuliert: \s
                `IST LEER E-Mail-Adresse`
                
                **Ergebnis:**
                - **Bedingung wahr:** Wenn das Feld für die E-Mail-Adresse leer ist, ist das Ergebnis **wahr**. \s
                - **Bedingung falsch:** Wenn eine E-Mail-Adresse angegeben wurde, ist das Ergebnis **falsch**.
                
                # Wahrheitswerte der Funktion „Ist leer“
                - **IST LEER ""** (leerer Text) → **wahr** \s
                - **IST LEER null** → **wahr** \s
                - **IST LEER "Hallo"** → **falsch** \s
                - **IST LEER 123** → **falsch**
                
                # Wann verwenden Sie die Funktion „Ist leer“?
                Verwenden Sie **„Ist leer“**, um zu prüfen, ob ein Wert fehlt oder leer ist. \s
                Diese Funktion ist besonders hilfreich bei Formularen, um zu validieren, ob eine Person erforderliche Felder leer gelassen hat, oder um sicherzustellen, dass bestimmte Daten eingegeben wurden.
                """;
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var res = super.performEvaluation(data, args);
        return new NoCodeResult(!res.getValueAsBoolean());
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "„#0“ ist leer";
    }

    @Override
    protected String getEmptinessSuffix() {
        return "leer ist";
    }
}
