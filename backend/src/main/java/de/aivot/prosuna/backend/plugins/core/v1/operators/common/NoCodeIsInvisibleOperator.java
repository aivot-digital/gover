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
        return "Prüft den berechneten Zustand eines Formularelements auf „nicht sichtbar“.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Prüfen Sie, ob ein Formularelement laut seinem berechneten Sichtbarkeitszustand nicht sichtbar ist. So kann eine weitere Bedingung auf dessen Sichtbarkeit aufbauen.

                # Welche Werte werden benötigt?
                Die interne Kennung des Formularelements. Geben Sie diese bei „Formularelement“ frei als Text ein; es gibt keine vorgegebene Auswahl. Über „Andere Wertquelle verwenden …“ kann die Kennung auch aus einer Wertquelle oder Funktion stammen. Benötigt wird die Kennung, nicht der Inhalt des Formularelements.

                # Welches Ergebnis erhalten Sie?
                Ein Wahrheitswert: „Wahr“, wenn das Formularelement im berechneten Zustand nicht sichtbar ist, andernfalls „Falsch“.

                # Beispiel im No-Code-Editor
                1. Ermitteln Sie die interne Kennung des Formularelements, dessen Sichtbarkeit geprüft werden soll, beispielsweise `telefonnummer`.
                2. Wählen Sie „Ist nicht sichtbar“ als Funktion für eine Bedingung.
                3. Tragen Sie bei „Formularelement“ `telefonnummer` ein.

                Ist dieses Formularelement nicht sichtbar, ergibt die Bedingung „Wahr“. Verwenden Sie das Ergebnis beispielsweise für einen zusätzlichen Hinweis.

                # Was ist zu beachten?
                Geprüft wird der berechnete Sichtbarkeitszustand, nicht, ob das Formularelement gerade im sichtbaren Bildschirmausschnitt liegt. Ist zur Kennung kein Zustand vorhanden, wird das Formularelement als sichtbar behandelt. Eine unbekannte Kennung ergibt daher „Falsch“.
                """;
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        return new NoCodeResult(!super.performEvaluation(data, args).getValueAsBoolean());
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "das Formularelement #0 ist nicht sichtbar";
    }

    @Override
    protected String getVisibilitySuffix() {
        return "nicht sichtbar ist";
    }
}
