package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    public void validate(RootElement root, Map<String, Object> customerInput, Collection<String> value, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
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
            List<String> vales = value.stream().toList();

            fields.add(new ValuePdfRowDto(
                    getLabel(),
                    vales.get(0)
            ));

            for (int i = 1; i < options.size(); i++) {
                fields.add(new ValuePdfRowDto(
                        "",
                        vales.get(i)
                ));
            }
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
