package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.InputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CheckboxField extends InputElement<Boolean> {
    public CheckboxField(BaseElement parent, Map<String, Object> data) {
        super(data);
    }

    @Override
    public boolean isValid(Boolean value, String idPrefix) {
        return !Boolean.TRUE.equals(getRequired()) || Boolean.TRUE.equals(value);
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Boolean value, String idPrefix) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        fields.add(new ValuePdfRowDto(
                getLabel(),
                Boolean.TRUE.equals(value) ? "Ja" : "Nein"
        ));

        return fields;
    }
}
