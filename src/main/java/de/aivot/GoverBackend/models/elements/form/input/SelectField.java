package de.aivot.GoverBackend.models.elements.form.input;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.form.InputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;

import javax.script.ScriptEngine;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SelectField extends InputElement<String> {
    private String placeholder;
    private Collection<String> options;

    public SelectField(Map<String, Object> data) {
        super(data);

        placeholder = (String) data.get("placeholder");
        options = (Collection<String>) data.get("options");
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Nullable
    public Collection<String> getOptions() {
        return options;
    }

    public void setOptions(Collection<String> options) {
        this.options = options;
    }

    @Override
    public void validate(Map<String, Object> customerInput, String value, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        testValueInOptions(value);
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, String value, String idPrefix, ScriptEngine scriptEngine) {
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
}
