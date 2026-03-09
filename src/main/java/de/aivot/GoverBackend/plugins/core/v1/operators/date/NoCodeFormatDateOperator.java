package de.aivot.GoverBackend.plugins.core.v1.operators.date;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeParameterOption;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class NoCodeFormatDateOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "format-date";
    }

    @Override
    public String getLabel() {
        return "Datum formatieren";
    }

    @Override
    public String getAbstract() {
        return "Formatiert ein Datum gemäß eines angegebenen Formats.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Datum formatieren“** formatiert ein Datum nach einem frei definierbaren Muster.
                
                # Anwendungsbeispiel:
                Angenommen, Sie haben ein Datum und möchten es als `TT.MM.JJJJ` ausgeben.
                
                ```text
                Datum formatieren (Datum, "dd.MM.yyyy")
                ```
                
                **Ergebnis:**
                Ein Text im gewünschten Datumsformat, z. B. `15.08.2023`.
                
                # Wann verwenden Sie den Operator „Datum formatieren“?
                Verwenden Sie **„Datum formatieren“**, wenn Sie:
                - Datumswerte als Text anzeigen oder weitergeben möchten.
                - Ein bestimmtes Ausgabeformat für Datum/Uhrzeit benötigen.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.String,
                        new NoCodeParameter(
                                NoCodeDataType.Date,
                                "Datum",
                                "Das zu formatierende Datum."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Format",
                                "Das Ausgabeformat als DateTimeFormatter-Muster.",
                                new NoCodeParameterOption("Datum (TT.MM.JJJJ)", "dd.MM.yyyy"),
                                new NoCodeParameterOption("Datum mit Zeit", "dd.MM.yyyy HH:mm"),
                                new NoCodeParameterOption("ISO Datum/Zeit", "yyyy-MM-dd'T'HH:mm:ssXXX")
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "formatiere „#0“ mit dem Muster „#1“";
    }

    @Override
    public NoCodeResult performEvaluation(ElementData data, Object... args) throws NoCodeException {
        var date = castToDateTime(args[0]);
        var formatPattern = castToString(args[1]).trim();

        if (formatPattern.isEmpty()) {
            throw new NoCodeException("Das Datumsformat darf nicht leer sein.");
        }

        final DateTimeFormatter formatter;
        try {
            formatter = DateTimeFormatter
                    .ofPattern(formatPattern, Locale.GERMAN)
                    .withZone(ZoneId.systemDefault());
        } catch (Exception e) {
            throw new NoCodeException("Ungültiges Datumsformat: " + formatPattern);
        }

        return new NoCodeResult(date.format(formatter));
    }
}
