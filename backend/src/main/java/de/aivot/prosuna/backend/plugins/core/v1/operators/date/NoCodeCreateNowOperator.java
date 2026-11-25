package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

public class NoCodeCreateNowOperator extends NoCodeOperator {
    private final BusinessTime businessTime;

    public NoCodeCreateNowOperator(BusinessTime businessTime) {
        this.businessTime = businessTime;
    }

    @Override
    public String getIdentifier() {
        return "create-now";
    }

    @Override
    public String getLabel() {
        return "Aktueller Zeitpunkt";
    }

    @Override
    public String getAbstract() {
        return "Liefert das aktuelle Datum mit Uhrzeit.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Mit „Aktueller Zeitpunkt“ verwenden Sie das aktuelle Datum mit Uhrzeit, beispielsweise um zu prüfen, ob ein geplanter Termin bereits erreicht ist.

                # Welche Werte werden benötigt?

                Die Funktion benötigt keine Eingabewerte. Nach dem Hinzufügen sind keine weiteren Einstellungen erforderlich.

                # Welches Ergebnis erhalten Sie?

                Sie erhalten einen Zeitpunkt mit Datum und Uhrzeit für den Moment der Auswertung.

                # Beispiel im No-Code-Editor

                Sie möchten prüfen, ob ein Termin bereits erreicht oder überschritten ist.

                1. Wählen Sie die Funktion „Zeitpunkte vergleichen“.
                2. Wählen Sie für „Zeitpunkt“ die Option „Aus Formularelement übernehmen“ und anschließend ein Datums- und Zeitfeld, beispielsweise „Termin“.
                3. Wählen Sie für „Vergleichsart“ die Option „früher als oder gleich“.
                4. Wählen Sie für „Vergleichszeitpunkt“ die Option „Mit Funktion ermitteln“ und anschließend „Aktueller Zeitpunkt“.

                Angenommen, der Ausdruck wird am 22.11.2026 um 13:30 Uhr ausgewertet:

                - Ein Termin am selben Tag um **13:00 Uhr** ergibt **Wahr**.
                - Ein Termin am selben Tag um **14:00 Uhr** ergibt **Falsch**.

                # Was ist zu beachten?

                Der Zeitpunkt wird bei jeder Auswertung neu ermittelt. Innerhalb einer Auswertung liefern alle Vorkommen von „Aktueller Zeitpunkt“ denselben Zeitpunkt; zwei Vorkommen in einem Ausdruck sind deshalb immer gleich. Die Darstellung von Datum und Uhrzeit richtet sich nach der für Prosuna eingestellten Zeitzone.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(NoCodeSignatur.of(NoCodeDataType.DateTime));
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "aktueller Zeitpunkt";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        return new NoCodeResult(data.getEvaluationInstant(businessTime.clock()));
    }
}
