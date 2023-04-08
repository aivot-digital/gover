package de.aivot.GoverBackend.models.elements.form.input;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.form.InputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;

import javax.script.ScriptEngine;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NumberField extends InputElement<Double> {
    private String placeholder;
    private Integer decimalPlaces;
    private String suffix;

    public NumberField(Map<String, Object> data) {
        super(data);

        placeholder = (String) data.get("placeholder");
        decimalPlaces = (Integer) data.get("decimalPlaces");
        suffix = (String) data.get("suffix");
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Nullable
    public Integer getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(Integer decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    @Nullable
    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public void validate(Map<String, Object> customerInput, Double value, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        // TODO
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, Double value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        String displayValue = "Keine Angaben";

        if (value != null) {
            int decimalPlaces = this.decimalPlaces != null ? this.decimalPlaces : 0;

            displayValue = String.format("%." + decimalPlaces + "f", value);

            if (suffix != null) {
                displayValue += " " + getSuffix();
            }
        }

        fields.add(new ValuePdfRowDto(
                getLabel(),
                displayValue
        ));

        return fields;
    }
}
