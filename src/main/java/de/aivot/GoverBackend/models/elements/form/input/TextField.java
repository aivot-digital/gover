package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.form.InputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;
import org.springframework.lang.Nullable;

import javax.script.ScriptEngine;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TextField extends InputElement<String> {
    private String placeholder;
    private Boolean isMultiline;
    private Integer maxCharacters;

    public TextField(Map<String, Object> data) {
        super(data);

        placeholder = (String) data.get("placeholder");
        isMultiline = (Boolean) data.get("isMultiline");
        maxCharacters = (Integer) data.get("maxCharacters");
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Nullable
    public Boolean getMultiline() {
        return isMultiline;
    }

    public void setMultiline(Boolean multiline) {
        isMultiline = multiline;
    }

    @Nullable
    public Integer getMaxCharacters() {
        return maxCharacters;
    }

    public void setMaxCharacters(Integer maxCharacters) {
        this.maxCharacters = maxCharacters;
    }

    @Override
    public void validate(Map<String, Object> customerInput, String value, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        if (maxCharacters != null && value.length() <= maxCharacters) {
            throw new ValidationException(this, "Too many characters");
        }
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
}
