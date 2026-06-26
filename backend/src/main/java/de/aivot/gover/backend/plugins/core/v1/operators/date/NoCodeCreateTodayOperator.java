package de.aivot.gover.backend.plugins.core.v1.operators.date;

import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.nocode.enums.NoCodeDataType;
import de.aivot.gover.backend.nocode.exceptions.NoCodeException;
import de.aivot.gover.backend.nocode.models.NoCodeOperator;
import de.aivot.gover.backend.nocode.models.NoCodeResult;
import de.aivot.gover.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public class NoCodeCreateTodayOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "create-today";
    }

    @Override
    public String getLabel() {
        return "Erstelle Heutiges Datum";
    }

    @Override
    public String getAbstract() {
        return "Erstellt das heutige Datum um Mitternacht.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Erstelle Heutiges Datum“** erstellt das heutige Datum um Mitternacht. \s
                Er wird verwendet, um das aktuelle Datum zu erhalten.
                
                # Anwendungsbeispiel:
                Angenommen, Sie möchten das heutige Datum erhalten. \s
                Mit dem Operator **„Erstelle Heutiges Datum“** können Sie das aktuelle Datum erstellen:
                
                ```text
                Erstelle Heutiges Datum ()
                ```
                
                **Ergebnis:** \s
                Das erstellte Datum ist das heutige Datum um Mitternacht.
                
                # Wann verwenden Sie den Operator „Erstelle Heutiges Datum“?
                Verwenden Sie **„Erstelle Heutiges Datum“**, wenn Sie:
                - Das aktuelle Datum benötigen. \s
                - Ein Datum für Berechnungen oder Vergleiche erstellen möchten.
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
        return "erstelle das heutige Datum";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var today = ZonedDateTime
                .now()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        return new NoCodeResult(today);
    }
}
