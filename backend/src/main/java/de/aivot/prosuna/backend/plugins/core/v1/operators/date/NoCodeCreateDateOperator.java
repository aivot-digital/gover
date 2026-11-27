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
        return "Datum bilden";
    }

    @Override
    public String getAbstract() {
        return "Bildet ein Datum aus einzeln angegebenem Tag, Monat und Jahr.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Bilden Sie ein Datum, wenn Tag, Monat und Jahr einzeln vorliegen, zum Beispiel für einen jährlich festgelegten Termin.

                # Welche Werte werden benötigt?
                - **Tag:** Der Tag des Monats als ganze Zahl.
                - **Monat:** Der gewünschte Monat.
                - **Jahr:** Das Jahr als ganze Zahl.

                Tag und Jahr können Sie selbst eingeben; dafür gibt es keine vorgegebenen Auswahlwerte. Für einen selbst festgelegten Monat wählen Sie einen der zwölf Monate aus. Zusätzlich können alle drei Eingaben aus einer anderen Wertquelle oder einer Funktion stammen.

                # Welches Ergebnis erhalten Sie?
                Ein vollständiges Datum mit Tag, Monat und Jahr. Dieses können Sie beispielsweise mit **„Datumswerte vergleichen“** prüfen oder mit **„Datum formatieren“** als Text ausgeben.

                # Beispiel im No-Code-Editor
                1. Wählen Sie im Eingabefeld **„Datum“** von **„Datumswerte vergleichen“** die Funktion **„Datum bilden“**.
                2. Geben Sie als **„Tag“** **15** ein, wählen Sie als **„Monat“** **„August“** und geben Sie als **„Jahr“** **2026** ein.
                3. Vergleichen Sie das Ergebnis beispielsweise mit einem Datum aus einem Formularelement.

                Das gebildete Datum ist **15.08.2026**. Zugeklappt lesen Sie: **„Datum aus Tag 15, Monat „August“ und Jahr 2026“**.

                # Was ist zu beachten?
                Die Kombination muss ein tatsächlich existierendes Datum ergeben. Der **31. Februar** wird beispielsweise abgelehnt. Das Jahr muss zwischen 1 und 9999 liegen. Die Funktion bildet einen Datumswert; für eine bestimmte Textdarstellung verwenden Sie **„Datum formatieren“**.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt das Datum";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Date,
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Tag",
                                "Der gewünschte Tag als ganze Zahl. Er muss im gewählten Monat und Jahr existieren."
                        ).withSentence("aus Tag"),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Monat",
                                "Wählen Sie den Monat aus. Bei einer anderen Wertquelle wird die Monatsnummer erwartet, zum Beispiel 8 für August.",
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
                        ).withSentence("Monat"),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Jahr",
                                "Das Jahr des gewünschten Datums von 1 bis 9999, zum Beispiel 2026."
                        ).withSentence("und Jahr")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "Datum aus Tag #0, Monat #1 und Jahr #2";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        int day = requireInteger(args[0], "Der Tag muss eine ganze Zahl sein.");
        int month = requireInteger(args[1], "Der Monat muss eine ganze Zahl sein.");
        int year = requireInteger(args[2], "Das Jahr muss eine ganze Zahl sein.");
        requireSupportedYear(year, "Das Jahr muss zwischen 1 und 9999 liegen.");

        final LocalDate date;
        try {
            date = LocalDate.of(year, month, day);
        } catch (java.time.DateTimeException exception) {
            throw new NoCodeException("Ungültiges Datum: " + day + "." + month + "." + year);
        }

        return new NoCodeResult(date);
    }
}
