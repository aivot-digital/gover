package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.*;

public class RadioField extends BaseInputElement<String> {
    private Collection<String> options;

    public RadioField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        options = MapUtils.getStringCollection(values, "options");
    }

    @Override
    protected String formatValue(Object value) {
        if (value instanceof String sValue) {
            return sValue;
        }
        return null;
    }

    @Override
    public void validate(RootElement root, Map<String, Object> customerInput, String value, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        testValueInOptions(value);
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, String value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        fields.add(new ValuePdfRowDto(
                getLabel(),
                value != null ? value : "Keine Angaben"
        ));

        return fields;
    }

    private void testValueInOptions(String value) throws ValidationException {
        boolean valueFound = false;
        for (String opt : options) {
            if (value.equals(opt)) {
                valueFound = true;
                break;
            }
        }
        if (!valueFound) {
            throw new ValidationException(this, "Invalid option " + value);
        }
    }

    @Override
    public boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        if (referencedValue == null) {
            return switch (operator) {
                case Equals -> comparedValue == null;
                case NotEquals -> comparedValue != null;
                case Empty -> true;
                default -> false;
            };
        }

        if (operator == ConditionOperator.NotEmpty) {
            return true;
        }

        if (referencedValue instanceof String sValA && comparedValue instanceof String sValB) {
            return switch (operator) {
                case Equals -> sValA.equals(sValB);
                case NotEquals -> !sValA.equals(sValB);

                default -> false;
            };
        }

        return false;
    }

    public Collection<String> getOptions() {
        return options;
    }

    public void setOptions(Collection<String> options) {
        this.options = options;
    }
}
