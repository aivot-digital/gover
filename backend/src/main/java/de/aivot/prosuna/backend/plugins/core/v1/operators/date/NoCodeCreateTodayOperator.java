package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.time.LocalDate;

public class NoCodeCreateTodayOperator extends NoCodeOperator {
    private final BusinessTime businessTime;

    public NoCodeCreateTodayOperator(BusinessTime businessTime) {
        this.businessTime = businessTime;
    }

    @Override
    public String getIdentifier() {
        return "create-today";
    }

    @Override
    public String getLabel() {
        return "Heutiges Datum";
    }

    @Override
    public String getAbstract() {
        return "Liefert das heutige Datum.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Mit „Heutiges Datum“ verwenden Sie das Datum des heutigen Tages in Berechnungen und Vergleichen, beispielsweise um einen Stichtag zu bestimmen.

                # Welche Werte werden benötigt?

                Die Funktion benötigt keine Eingabewerte. Nach dem Hinzufügen sind keine weiteren Einstellungen erforderlich.

                # Welches Ergebnis erhalten Sie?

                Sie erhalten das heutige Datum mit Tag, Monat und Jahr.

                # Beispiel im No-Code-Editor

                Sie möchten den Stichtag ermitteln, der 18 Jahre vor heute liegt.

                1. Wählen Sie die Funktion „Vom Datum abziehen“.
                2. Wählen Sie für „Ausgangsdatum“ die Option „Mit Funktion ermitteln“ und anschließend „Heutiges Datum“.
                3. Wählen Sie für „Anzahl“ die Option „Wert selbst festlegen“ und geben Sie `18` ein.
                4. Wählen Sie für „Einheit“ die Option „Jahre“.

                Der fertige Ausdruck liest sich so:

                **(heutiges Datum) minus 18 Jahre**

                Bei einer Auswertung am 22.11.2026 ist das Ergebnis **22.11.2008**.

                # Was ist zu beachten?

                Das Datum wird bei jeder Auswertung neu ermittelt. Innerhalb einer Auswertung liefern alle Vorkommen von „Heutiges Datum“ dasselbe Datum; zwei Vorkommen in einem Ausdruck sind deshalb immer gleich. Welcher Tag als heute gilt, richtet sich nach der für Prosuna eingestellten Zeitzone.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Date
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "heutiges Datum";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        return new NoCodeResult(LocalDate.ofInstant(data.getEvaluationInstant(businessTime.clock()), businessTime.zoneId()));
    }
}
