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

public class NoCodeExtractDateTimePartOperator extends NoCodeOperator {
    public enum Part {
        DATE,
        TIME,
    }

    private final Part part;
    private final BusinessTime businessTime;

    public NoCodeExtractDateTimePartOperator(Part part, BusinessTime businessTime) {
        this.part = part;
        this.businessTime = businessTime;
    }

    @Override
    public String getIdentifier() {
        return switch (part) {
            case DATE -> "extract-date-from-datetime";
            case TIME -> "extract-time-from-datetime";
        };
    }

    @Override
    public String getLabel() {
        return switch (part) {
            case DATE -> "Datum aus Zeitpunkt";
            case TIME -> "Uhrzeit aus Zeitpunkt";
        };
    }

    @Override
    public String getAbstract() {
        return switch (part) {
            case DATE -> "Ermittelt das Kalenderdatum eines Zeitpunkts in der Zeitzone der Anwendung.";
            case TIME -> "Ermittelt die lokale Uhrzeit eines Zeitpunkts in der Zeitzone der Anwendung.";
        };
    }

    @Override
    public String getDescription() {
        return getAbstract();
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        part == Part.DATE ? NoCodeDataType.Date : NoCodeDataType.Time,
                        new NoCodeParameter(
                                NoCodeDataType.DateTime,
                                "Zeitpunkt",
                                "Der absolute Zeitpunkt."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return switch (part) {
            case DATE -> "ermittle das Datum von „#0“";
            case TIME -> "ermittle die Uhrzeit von „#0“";
        };
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var instant = requireDateTime(
                args[0],
                "Ungültiger Zeitpunkt: " + castToString(args[0])
        ).toInstant();
        var applicationDateTime = instant.atZone(businessTime.zoneId());

        return new NoCodeResult(
                part == Part.DATE
                        ? applicationDateTime.toLocalDate()
                        // The public Time contract has second precision even when the
                        // source Instant contains a fractional second.
                        : applicationDateTime.toLocalTime().withNano(0)
        );
    }
}
