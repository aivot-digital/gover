package de.aivot.prosuna.backend.plugins.core.v1.operators.common;

import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

public class NoCodeIsVisibleOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "is-visible";
    }

    @Override
    public String getLabel() {
        return "Ist sichtbar";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob das angegebene Formularelement laut berechnetem Sichtbarkeitszustand sichtbar ist.";
    }

    @Override
    public String getDescription() {
        return """                
                # Beschreibung:
                Der Operator **„Ist sichtbar“** prüft den berechneten Sichtbarkeitszustand des angegebenen Formularelements. \s
                Geben Sie die **ID eines im Formular konfigurierten Formularelements** an. \s
                Der Operator gibt **wahr** (true) zurück, wenn der berechnete Zustand sichtbar ist, und **falsch** (false), wenn er nicht sichtbar ist.
                
                # Anwendungsbeispiel:
                Im Formular ist ein Eingabefeld mit der ID `"telefonnummer"` konfiguriert.
                Seine Sichtbarkeit wird über eine Bedingung gesteuert.
                
                Mit dem Operator **„Ist sichtbar“** prüfen Sie dessen berechneten Sichtbarkeitszustand: \s
                ```text
                Ist sichtbar("telefonnummer")
                ```
                
                **Ergebnis:**
                - **wahr:** Wenn das Eingabefeld im berechneten Zustand sichtbar ist. \s
                - **falsch:** Wenn das Eingabefeld im berechneten Zustand nicht sichtbar ist.
                
                **Weitere Beispiele:**
                - **Ist sichtbar("zusatzangaben"):** → **wahr**, wenn der im Formular konfigurierte Abschnitt `"zusatzangaben"` im berechneten Zustand sichtbar ist. \s
                - **Ist sichtbar("hinweis"):** → **falsch**, wenn das im Formular konfigurierte Textelement `"hinweis"` im berechneten Zustand nicht sichtbar ist.
                
                # Wann verwenden Sie den Operator „Ist sichtbar“?
                Verwenden Sie **„Ist sichtbar“**, um Bedingungen in Ihrem Formular vom berechneten Sichtbarkeitszustand eines anderen Formularelements abhängig zu machen.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Wert",
                                "Die ID des zu überprüfenden Formularelements."
                        )
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var arg = castToString(args[0]);
        var isVisible = data
                .getElementStates()
                .getOrDefault(arg, ComputedElementState.create())
                .getVisible();
        return new NoCodeResult(isVisible);
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "das Formularelement „#0“ ist sichtbar";
    }
}
