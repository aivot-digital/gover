package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.form.InputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;

import javax.script.ScriptEngine;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CheckboxField extends InputElement<Boolean> {
    public CheckboxField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void validate(Map<String, Object> customerInput, Boolean value, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        if (Boolean.TRUE.equals(getRequired()) && !Boolean.TRUE.equals(value)) {
            throw new RequiredValidationException(this);
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, Boolean value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        fields.add(new ValuePdfRowDto(
                getLabel(),
                Boolean.TRUE.equals(value) ? "Ja" : "Nein"
        ));

        return fields;
    }
}
