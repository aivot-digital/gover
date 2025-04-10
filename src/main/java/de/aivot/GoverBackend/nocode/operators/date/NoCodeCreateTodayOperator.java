package de.aivot.GoverBackend.nocode.operators.date;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

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
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{};
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.Date;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        var today = ZonedDateTime
                .now()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        return new NoCodeResult(NoCodeDataType.Date, today);
    }
}