package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;

import javax.script.ScriptEngine;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CheckboxField extends BaseInputElement<Boolean> {
    public CheckboxField(Map<String, Object> data) {
        super(data);
    }

    @Override
    protected Boolean formatValue(Object value) {
        return Boolean.valueOf(Boolean.TRUE.equals(value));
    }

    @Override
    public void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, Boolean value, ScriptEngine scriptEngine) throws ValidationException {
        if (Boolean.TRUE.equals(getRequired()) && !Boolean.TRUE.equals(value)) {
            throw new RequiredValidationException(this);
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, Boolean value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        fields.add(new ValuePdfRowDto(
                getLabel(),
                Boolean.TRUE.equals(value) ? "Ja" : "Nein",
                this
        ));

        return fields;
    }

    @Override
    public boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        boolean valA = transformValue(referencedValue);
        boolean valB = transformValue(comparedValue);

        return switch (operator) {
            case Equals -> valA == valB;
            case NotEquals -> valA != valB;

            case Empty -> !valA;
            case NotEmpty -> valA;

            default -> false;
        };
    }

    private boolean transformValue(Object value) {
        if (value instanceof String sValue) {
            return "Ja (True)".equals(sValue);
        }

        if (value instanceof Boolean bValue) {
            return bValue;
        }

        return false;
    }
}
