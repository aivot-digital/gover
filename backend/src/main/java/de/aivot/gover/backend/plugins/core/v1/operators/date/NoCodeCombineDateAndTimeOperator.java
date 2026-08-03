package de.aivot.gover.backend.plugins.core.v1.operators.date;

import de.aivot.gover.backend.core.services.BusinessTime;
import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.nocode.enums.NoCodeDataType;
import de.aivot.gover.backend.nocode.exceptions.NoCodeException;
import de.aivot.gover.backend.nocode.models.NoCodeOperator;
import de.aivot.gover.backend.nocode.models.NoCodeParameter;
import de.aivot.gover.backend.nocode.models.NoCodeResult;
import de.aivot.gover.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class NoCodeCombineDateAndTimeOperator extends NoCodeOperator {
    private final BusinessTime businessTime;

    public NoCodeCombineDateAndTimeOperator(BusinessTime businessTime) {
        this.businessTime = businessTime;
    }

    @Override
    public String getIdentifier() {
        return "combine-date-and-time";
    }

    @Override
    public String getLabel() {
        return "Datum und Uhrzeit verbinden";
    }

    @Override
    public String getAbstract() {
        return "Verbindet ein vollständiges Datum und eine lokale Uhrzeit zu einem absoluten Zeitpunkt.";
    }

    @Override
    public String getDescription() {
        return """
                Der Operator verbindet ein vollständiges Kalenderdatum und eine lokale Uhrzeit.
                Die lokale Datums- und Uhrzeitangabe wird in der Zeitzone der Anwendung aufgelöst und als absoluter Zeitpunkt zurückgegeben.
                Nicht existierende lokale Uhrzeiten während einer DST-Umstellung werden abgelehnt.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.DateTime,
                        new NoCodeParameter(
                                NoCodeDataType.Date,
                                "Datum",
                                "Das vollständige Kalenderdatum."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Time,
                                "Uhrzeit",
                                "Die lokale Uhrzeit ohne Zeitzone."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "verbinde Datum „#0“ und Uhrzeit „#1“";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var calendarValue = requireCalendarValue(
                args[0],
                "Ein vollständiges Datum im Format JJJJ-MM-TT wird benötigt."
        );
        if (!(calendarValue instanceof LocalDate date)) {
            // Resolving Year or YearMonth would invent calendar components that the
            // user never supplied, so only day-precision values can become an instant.
            throw new NoCodeException("Ein vollständiges Datum im Format JJJJ-MM-TT wird benötigt.");
        }
        var time = requireTime(args[1], "Ungültige Uhrzeit: " + castToString(args[1]));

        try {
            return new NoCodeResult(businessTime.resolve(LocalDateTime.of(date, time)));
        } catch (DateTimeException exception) {
            throw new NoCodeException(
                    "Die lokale Datums- und Uhrzeitangabe %s existiert in der Zeitzone der Anwendung (%s) nicht."
                            .formatted(LocalDateTime.of(date, time), businessTime.zoneId())
            );
        }
    }
}
