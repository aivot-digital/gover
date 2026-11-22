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
        return "Prüft den berechneten Zustand eines Formularelements auf „sichtbar“.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Prüfen Sie, ob ein Formularelement laut seinem berechneten Sichtbarkeitszustand sichtbar ist. So kann eine weitere Bedingung auf dessen Sichtbarkeit aufbauen.

                # Welche Werte werden benötigt?
                Die interne Kennung des Formularelements. Geben Sie diese bei „Formularelement“ frei als Text ein; es gibt keine vorgegebene Auswahl. Über „Andere Wertquelle verwenden …“ kann die Kennung auch aus einer Wertquelle oder Funktion stammen. Benötigt wird die Kennung, nicht der Inhalt des Formularelements.

                # Welches Ergebnis erhalten Sie?
                Ein Wahrheitswert: „Wahr“, wenn das Formularelement im berechneten Zustand sichtbar ist, andernfalls „Falsch“.

                # Beispiel im No-Code-Editor
                1. Ermitteln Sie die interne Kennung des Formularelements, dessen Sichtbarkeit geprüft werden soll, beispielsweise `telefonnummer`.
                2. Wählen Sie „Ist sichtbar“ als Funktion für eine Bedingung.
                3. Tragen Sie bei „Formularelement“ `telefonnummer` ein.

                Ist dieses Formularelement sichtbar, ergibt die Bedingung „Wahr“. Verwenden Sie das Ergebnis beispielsweise für einen zusätzlichen Hinweis.

                # Was ist zu beachten?
                Geprüft wird der berechnete Sichtbarkeitszustand, nicht, ob das Formularelement gerade im sichtbaren Bildschirmausschnitt liegt. Ist zur Kennung kein Zustand vorhanden, wird das Formularelement als sichtbar behandelt. Eine unbekannte Kennung ergibt daher „Wahr“.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }

    /** The phrase after the field, so subclasses keep the sentence of this operator. */
    protected String getVisibilitySuffix() {
        return "sichtbar ist";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Formularelement",
                                "Die interne Kennung des Formularelements, dessen Sichtbarkeitszustand geprüft wird."
                        ).withSentence("wenn das Formularelement", getVisibilitySuffix())
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
        return "das Formularelement #0 ist sichtbar";
    }
}
