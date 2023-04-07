package de.aivot.GoverBackend.models.elements.form.input;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.InputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MultiCheckboxField extends InputElement<Collection<String>> {
    private Collection<String> options;
    private Integer minimumRequiredOptions;

    public MultiCheckboxField(BaseElement parent, Map<String, Object> data) {
        super(data);

        options = (Collection<String>) data.get("options");
        minimumRequiredOptions = (Integer) data.get("minimumRequiredOptions");
    }

    @Nullable
    public Collection<String> getOptions() {
        return options;
    }

    public void setOptions(Collection<String> options) {
        this.options = options;
    }

    @Nullable
    public Integer getMinimumRequiredOptions() {
        return minimumRequiredOptions;
    }

    public void setMinimumRequiredOptions(Integer minimumRequiredOptions) {
        this.minimumRequiredOptions = minimumRequiredOptions;
    }

    @Override
    public boolean isValid(Collection<String> value, String idPrefix) {
        return testValuesInOptions(value) && testRequiredOptionsMet(value);
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Collection<String> value, String idPrefix) {
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

    private boolean testValuesInOptions(Collection<String> values) {
        for (String val : values) {
            boolean valueFound = false;
            for (String opt : options) {
                if (val.equals(opt)) {
                    valueFound = true;
                    break;
                }
            }
            if (!valueFound) {
                return false;
            }
        }
        return true;
    }

    private boolean testRequiredOptionsMet(Collection<String> values) {
        return minimumRequiredOptions == null || values.size() >= minimumRequiredOptions;
    }
}
