package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import jakarta.annotation.Nullable;

public class NoCodeIsInvisibleOperator extends NoCodeIsVisibleOperator {
    @Override
    public String getIdentifier() {
        return "is-invisible";
    }

    @Override
    public String getLabel() {
        return "Ist nicht sichtbar";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob das angegebene Formularelement laut berechnetem Sichtbarkeitszustand nicht sichtbar ist.";
    }

    @Override
    public String getDescription() {
        return """                
                ### **Operator: "Ist nicht sichtbar"**
                
                # Beschreibung:
                Der Operator **„Ist nicht sichtbar“** prüft den berechneten Sichtbarkeitszustand des angegebenen Formularelements. \s
                Geben Sie die **ID eines im Formular konfigurierten Formularelements** an. \s
                Der Operator gibt **wahr** (true) zurück, wenn der berechnete Zustand nicht sichtbar ist, und **falsch** (false), wenn er sichtbar ist.
                
                # Anwendungsbeispiel:
                Im Formular ist ein Eingabefeld mit der ID `"telefonnummer"` konfiguriert.
                Seine Sichtbarkeit wird über eine Bedingung gesteuert.
                
                Mit dem Operator **„Ist nicht sichtbar“** prüfen Sie dessen berechneten Sichtbarkeitszustand: \s
                ```text
                Ist nicht sichtbar("telefonnummer")
                ```
                
                **Ergebnis:**
                - **wahr:** Wenn das Eingabefeld im berechneten Zustand nicht sichtbar ist. \s
                - **falsch:** Wenn das Eingabefeld im berechneten Zustand sichtbar ist.
                
                **Weitere Beispiele:**
                - **Ist nicht sichtbar("zusatzangaben"):** → **falsch**, wenn der im Formular konfigurierte Abschnitt `"zusatzangaben"` im berechneten Zustand sichtbar ist. \s
                - **Ist nicht sichtbar("hinweis"):** → **wahr**, wenn das im Formular konfigurierte Textelement `"hinweis"` im berechneten Zustand nicht sichtbar ist.
                
                # Wann verwenden Sie den Operator „Ist nicht sichtbar“?
                Verwenden Sie **„Ist nicht sichtbar“**, um Bedingungen in Ihrem Formular vom berechneten Sichtbarkeitszustand eines anderen Formularelements abhängig zu machen.
                """;
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        return new NoCodeResult(!super.performEvaluation(data, args).getValueAsBoolean());
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "das Formularelement „#0“ ist nicht sichtbar";
    }
}
