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

                - **Formularelement:** Das Formularelement, dessen Sichtbarkeit geprüft wird. Die Formularelemente des Formulars beziehungsweise der UI-Definition werden zur Auswahl angeboten. Gespeichert wird die interne Kennung des gewählten Formularelements, nicht sein Inhalt. Über **„Andere Wertquelle verwenden …“** kann die Kennung auch aus einer Wertquelle oder Funktion stammen.

                # Welches Ergebnis erhalten Sie?

                Ein Wahrheitswert: `Wahr`, wenn das Formularelement im berechneten Zustand nicht sichtbar ist, andernfalls `Falsch`.

                # Beispiel im No-Code-Editor

                1. Wählen Sie **„Ist nicht sichtbar“** als Funktion für eine Bedingung.
                2. Wählen Sie bei **„Formularelement“** das Formularelement aus, dessen Sichtbarkeit geprüft werden soll, beispielsweise **„Telefonnummer“**.

                Im fertigen Ausdruck sehen Sie den Namen **„Telefonnummer“**; gespeichert wird die interne Kennung des Formularelements. Ist dieses Formularelement nicht sichtbar, ergibt die Bedingung `Wahr`. Verwenden Sie das Ergebnis beispielsweise für einen zusätzlichen Hinweis.

                # Was ist zu beachten?

                Geprüft wird der berechnete Sichtbarkeitszustand, nicht, ob das Formularelement gerade im sichtbaren Bildschirmausschnitt liegt. Ist für das Formularelement kein berechneter Zustand vorhanden, wird es als sichtbar behandelt; die Funktion ergibt dann `Falsch`. Passt die gespeicherte Kennung zu keinem Formularelement, weist der Editor mit **„Das ausgewählte Formularelement ist nicht verfügbar.“** darauf hin.

                Die Funktion wird nur dort angeboten, wo Formularelemente einen berechneten Zustand haben, etwa in **„Sichtbarkeit“**, **„Validierung“** und **„Dynamischer Wert“** von Formularelementen in Formularen und UI-Definitionen. In der Konfiguration von Prozesselementen, etwa für Verzweigungen, steht sie nicht zur Verfügung.
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
