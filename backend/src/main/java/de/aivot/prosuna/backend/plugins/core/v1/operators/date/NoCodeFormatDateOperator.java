package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameterOption;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class NoCodeFormatDateOperator extends NoCodeOperator {
    private final BusinessTime businessTime;

    public NoCodeFormatDateOperator(BusinessTime businessTime) {
        this.businessTime = businessTime;
    }

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
                                new NoCodeParameterOption("Monat und Jahr", "MM.yyyy"),
                                new NoCodeParameterOption("Jahr", "yyyy"),
                                new NoCodeParameterOption("ISO-Datum", "yyyy-MM-dd")
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
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var formatter = requireFormatter(args[1]);

        try {
            var calendarValue = tryCastToCalendarValue(args[0]);
            if (calendarValue != null) {
                // Formatting the original type preserves Year and YearMonth precision.
                // Incompatible fields (for example "dd" on YearMonth) fail explicitly.
                return new NoCodeResult(formatter.format(calendarValue));
            }

            // Existing format-date expressions historically accepted DateTime values at
            // runtime despite declaring a Date parameter. Preserve that behavior while
            // the dedicated format-datetime operator provides the correct editor contract.
            return formatDateTimeValue(
                    args[0],
                    formatter,
                    "Ungültiger Datumswert: " + castToString(args[0])
            );
        } catch (DateTimeException exception) {
            throw incompatibleFormat();
        }
    }

    protected final DateTimeFormatter requireFormatter(Object patternValue) throws NoCodeException {
        var formatPattern = castToString(patternValue).trim();

        if (formatPattern.isEmpty()) {
            throw new NoCodeException("Das Datumsformat darf nicht leer sein.");
        }

        try {
            return DateTimeFormatter.ofPattern(formatPattern, Locale.GERMAN);
        } catch (IllegalArgumentException exception) {
            throw new NoCodeException("Ungültiges Datumsformat: " + formatPattern);
        }
    }

    protected final NoCodeResult formatDateTimeValue(
            Object value,
            DateTimeFormatter formatter,
            String invalidValueMessage
    ) throws NoCodeException {
        var instant = requireDateTime(value, invalidValueMessage).toInstant();

        try {
            return new NoCodeResult(formatter.format(instant.atZone(businessTime.zoneId())));
        } catch (DateTimeException exception) {
            throw incompatibleFormat();
        }
    }

    private NoCodeException incompatibleFormat() {
        return new NoCodeException(
                "Das gewählte Format ist für den übergebenen Datumswert nicht anwendbar."
        );
    }
}
