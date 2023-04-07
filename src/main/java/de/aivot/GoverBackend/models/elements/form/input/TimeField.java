package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.InputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TimeField extends InputElement<String> {
    public TimeField(BaseElement parent, Map<String, Object> data) {
        super(data);
    }

    @Override
    public boolean isValid(String value, String idPrefix) {
        try {
            LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            return false;
        }
        return true;
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(String value, String idPrefix) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        String valueText = "Keine Angaben";
        try {
            ZonedDateTime time = ZonedDateTime.parse((String) value, DateTimeFormatter.ISO_DATE_TIME);
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
