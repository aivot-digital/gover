package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.exceptions.ValidationException;
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

        options = MapUtils.get(values, "options", Collection.class);
        minimumRequiredOptions = MapUtils.getInteger(values, "minimumRequiredOptions");
    }

    @Override
    public void validate(Map<String, Object> customerInput, Collection<String> value, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        testValuesInOptions(value);
        testRequiredOptionsMet(value);
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, Collection<String> value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        if (options == null || options.isEmpty()) {
            fields.add(new ValuePdfRowDto(
                    getLabel(),
                    "Keine Angaben"
            ));
        } else {
            List<String> options = value.stream().toList();

            fields.add(new ValuePdfRowDto(
                    getLabel(),
                    options.get(0)
            ));

            for (int i = 1; i < options.size(); i++) {
                fields.add(new ValuePdfRowDto(
                        "",
                        options.get(i)
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
        if (minimumRequiredOptions != null && values.size() >= minimumRequiredOptions) {
            throw new ValidationException(this, "Not enough options selected");
        }
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
