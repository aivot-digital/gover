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

public class MultiCheckboxField extends BaseInputElement<Collection<String>> {
    private Collection<String> options;
    private Integer minimumRequiredOptions;

    public MultiCheckboxField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        options = MapUtils.getStringCollection(values, "options");
        minimumRequiredOptions = MapUtils.getInteger(values, "minimumRequiredOptions");
    }

    @Override
    protected Collection<String> formatValue(Object value) {
        Collection<String> res = new LinkedList<>();
        if (value instanceof Collection<?> cValue) {
            for (Object val : cValue) {
                if (val instanceof String sVal) {
                    res.add(sVal);
                }
            }
        }
        return res.isEmpty() ? null : res;
    }

    @Override
    public void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, Collection<String> value, ScriptEngine scriptEngine) throws ValidationException {
        testValuesInOptions(value);
        testRequiredOptionsMet(value);
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, Collection<String> value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        if (value == null || value.isEmpty()) {
            fields.add(new ValuePdfRowDto(
                    getLabel(),
                    "Keine Angaben"
            ));
        } else {
            StringBuilder sb = new StringBuilder();

            for (String opt : value) {
                sb.append(opt);
                sb.append("<br/>");
            }

            fields.add(new ValuePdfRowDto(
                    getLabel(),
                    sb.toString()
            ));
        }

        return fields;
    }

    private void testValuesInOptions(Collection<String> values) throws ValidationException {
        for (String val : values) {
            boolean valueFound = false;
            for (String opt : options) {
                if (val.equals(opt)) {
                    valueFound = true;
                    break;
                }
            }
            if (!valueFound) {
                throw new ValidationException(this, "Invalid option " + val);
            }
        }
    }

    private void testRequiredOptionsMet(Collection<String> values) throws ValidationException {
        if (minimumRequiredOptions != null && values.size() < minimumRequiredOptions) {
            throw new ValidationException(this, "Not enough options selected");
        }
    }

    @Override
    public boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        if (referencedValue == null) {
            return operator == ConditionOperator.Empty;
        }

        if (operator == ConditionOperator.NotEmpty) {
            return true;
        }

        if (referencedValue instanceof Collection<?> cValA) {
            if (comparedValue instanceof String sValueB) {
                return switch (operator) {
                    case Includes -> cValA.stream().anyMatch(sValueB::equals);
                    case NotIncludes -> cValA.stream().noneMatch(sValueB::equals);
                    default -> false;
                };
            }
        }

        return false;
    }

    public Collection<String> getOptions() {
        return options;
    }

    public void setOptions(Collection<String> options) {
        this.options = options;
    }

    public Integer getMinimumRequiredOptions() {
        return minimumRequiredOptions;
    }

    public void setMinimumRequiredOptions(Integer minimumRequiredOptions) {
        this.minimumRequiredOptions = minimumRequiredOptions;
    }
}
