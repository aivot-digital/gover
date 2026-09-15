package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import jakarta.annotation.Nullable;

import java.time.LocalDate;

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
                Das erstellte Datum ist: `2023-08-15`.
                
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

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "erstelle ein Datum aus Tag „#0“, Monat „#1“ und Jahr „#2“";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        int day = requireInteger(args[0], "Der Tag muss eine ganze Zahl sein.");
        int month = requireInteger(args[1], "Der Monat muss eine ganze Zahl sein.");
        int year = requireInteger(args[2], "Das Jahr muss eine ganze Zahl sein.");

        final LocalDate date;
        try {
            date = LocalDate.of(year, month, day);
        } catch (java.time.DateTimeException exception) {
            throw new NoCodeException("Ungültiges Datum: " + day + "." + month + "." + year);
        }

        return new NoCodeResult(date);
    }
}
