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

public class RadioField extends InputElement<String> {
    private Collection<String> options;

    public RadioField(BaseElement parent, Map<String, Object> data) {
        super(data);

        options = (Collection<String>) data.get("options");
    }

    @Nullable
    public Collection<String> getOptions() {
        return options;
    }

    public void setOptions(Collection<String> options) {
        this.options = options;
    }

    @Override
    public boolean isValid(String value, String idPrefix) {
        return testValueInOptions(value);
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(String value, String idPrefix) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        fields.add(new ValuePdfRowDto(
                getLabel(),
                value != null ? value : "Keine Angaben"
        ));

        return fields;
    }

    private boolean testValueInOptions(String value) {
        boolean valueFound = false;
        for (String opt : options) {
            if (value.equals(opt)) {
                valueFound = true;
                break;
            }
        }
        return valueFound;
    }
}
