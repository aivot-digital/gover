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

                - **Formularelement:** Das Formularelement, dessen Sichtbarkeit geprüft wird. Die Formularelemente des Formulars beziehungsweise der UI-Definition werden zur Auswahl angeboten. Gespeichert wird die interne Kennung des gewählten Formularelements, nicht sein Inhalt. Über **„Andere Wertquelle verwenden …“** kann die Kennung auch aus einer Wertquelle oder Funktion stammen.

                # Welches Ergebnis erhalten Sie?

                Ein Wahrheitswert: `Wahr`, wenn das Formularelement im berechneten Zustand sichtbar ist, andernfalls `Falsch`.

                # Beispiel im No-Code-Editor

                1. Wählen Sie **„Ist sichtbar“** als Funktion für eine Bedingung.
                2. Wählen Sie bei **„Formularelement“** das Formularelement aus, dessen Sichtbarkeit geprüft werden soll, beispielsweise **„Telefonnummer“**.

                Im fertigen Ausdruck sehen Sie den Namen **„Telefonnummer“**; gespeichert wird die interne Kennung des Formularelements. Ist dieses Formularelement sichtbar, ergibt die Bedingung `Wahr`. Verwenden Sie das Ergebnis beispielsweise für einen zusätzlichen Hinweis.

                # Was ist zu beachten?

                Geprüft wird der berechnete Sichtbarkeitszustand, nicht, ob das Formularelement gerade im sichtbaren Bildschirmausschnitt liegt. Ist für das Formularelement kein berechneter Zustand vorhanden, wird es als sichtbar behandelt; die Funktion ergibt dann `Wahr`. Passt die gespeicherte Kennung zu keinem Formularelement, weist der Editor mit **„Das ausgewählte Formularelement ist nicht verfügbar.“** darauf hin.

                Die Funktion wird nur dort angeboten, wo Formularelemente einen berechneten Zustand haben, etwa in **„Sichtbarkeit“**, **„Validierung“** und **„Dynamischer Wert“** von Formularelementen in Formularen und UI-Definitionen. In der Konfiguration von Prozesselementen, etwa für Verzweigungen, steht sie nicht zur Verfügung.
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
                                "Das Formularelement, dessen Sichtbarkeitszustand geprüft wird. Gespeichert wird seine interne Kennung."
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
