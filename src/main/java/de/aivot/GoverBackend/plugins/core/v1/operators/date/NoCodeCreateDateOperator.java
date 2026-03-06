package de.aivot.GoverBackend.plugins.core.v1.operators.date;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class NoCodeCreateDateOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "create-date";
    }

    @Override
    public String getLabel() {
        return "Erstelle Datum";
    }

    @Override
    public String getAbstract() {
        return "Erstellt ein Datum basierend auf den angegebenen Tag, Monat und Jahr.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Erstelle Datum“** erstellt ein Datum basierend auf den angegebenen Tag, Monat und Jahr. \s
                Er wird verwendet, um ein Datum aus einzelnen Komponenten zu erstellen.
                
                # Anwendungsbeispiel:
                Angenommen, Sie haben die Werte für Tag, Monat und Jahr: `15`, `8`, `2023`. \s
                Mit dem Operator **„Erstelle Datum“** können Sie ein Datum erstellen:
                
                ```text
                Erstelle Datum (15, 8, 2023)
                ```
                
                **Ergebnis:** \s
                Das erstellte Datum ist: `2023-08-15T00:00:00Z`.
                
                # Wann verwenden Sie den Operator „Erstelle Datum“?
                Verwenden Sie **„Erstelle Datum“**, wenn Sie:
                - Ein Datum aus einzelnen Komponenten (Tag, Monat, Jahr) erstellen möchten. \s
                - Daten in einem bestimmten Format benötigen. \s
                - Ein Datum für Berechnungen oder Vergleiche erstellen möchten.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Date,
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Tag",
                                "Der Tag des Monats (1-31)"
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Monat",
                                "Der Monat des Jahres (1-12)",
                                new NoCodeParameterOption("Januar", "1"),
                                new NoCodeParameterOption("Februar", "2"),
                                new NoCodeParameterOption("März", "3"),
                                new NoCodeParameterOption("April", "4"),
                                new NoCodeParameterOption("Mai", "5"),
                                new NoCodeParameterOption("Juni", "6"),
                                new NoCodeParameterOption("Juli", "7"),
                                new NoCodeParameterOption("August", "8"),
                                new NoCodeParameterOption("September", "9"),
                                new NoCodeParameterOption("Oktober", "10"),
                                new NoCodeParameterOption("November", "11"),
                                new NoCodeParameterOption("Dezember", "12")
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Jahr",
                                "Das Jahr (z.B. 2023)"
                        )
                )
        );
    }

    @Override
    public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
        int day = castToNumber(args[0]).intValue();
        int month = castToNumber(args[1]).intValue();
        int year = castToNumber(args[2]).intValue();

        final ZonedDateTime date;
        try {
            date = ZonedDateTime.of(
                    year, month, day,
                    0, 0, 0, 0,
                    ZoneId.systemDefault()
            );
        } catch (Exception e) {
            throw new NoCodeException("Ungültiges Datum: " + day + "." + month + "." + year);
        }

        return new NoCodeResult(date);
    }
}
