package de.aivot.GoverBackend.elements.models.form.input;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.models.pdf.ValuePdfRowDto;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CheckboxField extends BaseInputElement<Boolean> {
    public CheckboxField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public Boolean formatValue(Object value) {
        if (value instanceof String sValue) {
            return "Ja (True)".equals(sValue);
        }

        if (value instanceof Boolean bValue) {
            return bValue;
        }

        return false;
    }

    @Override
    public void validate(Boolean value) throws ValidationException {
        if (Boolean.TRUE.equals(getRequired()) && !Boolean.TRUE.equals(value)) {
            throw new RequiredValidationException(this);
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Boolean value, String idPrefix, FormState formState) {
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
        boolean valA = formatValue(referencedValue);
        boolean valB = formatValue(comparedValue);

        return switch (operator) {
            case Equals -> valA == valB;
            case NotEquals -> valA != valB;

            case Empty -> !valA;
            case NotEmpty -> valA;

            default -> false;
        };
    }
}
