package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;

import javax.script.ScriptEngine;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TimeField extends BaseInputElement<String> {
    public TimeField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void validate(Map<String, Object> customerInput, String value, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
        } else {
            try {
                String cleanedTime = value.contains("T") ? value.split("T")[1] : value;
                LocalTime.parse(cleanedTime.replaceAll("Z", ""));
            } catch (DateTimeParseException e) {
                throw new ValidationException(this, "Failed to parse time:" + e.getMessage());
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, String value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        String valueText = "Keine Angaben";
        try {
            ZonedDateTime time = ZonedDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
            valueText = time.format(DateTimeFormatter
                    .ofPattern("hh:mm")
                    .withZone(ZoneId.of("Europe/Paris"))) + " Uhr";
        } catch (Exception e) {
            e.printStackTrace();
        }

        fields.add(new ValuePdfRowDto(
                getLabel(),
                valueText
        ));

        return fields;
    }
}
