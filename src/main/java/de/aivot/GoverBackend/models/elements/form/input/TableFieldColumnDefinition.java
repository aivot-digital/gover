package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.enums.TableColumnDataType;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;

public class TableFieldColumnDefinition {
    private String label;
    private TableColumnDataType datatype;
    private String placeholder;
    private Integer decimalPlaces;
    private Boolean optional;
    private Boolean disabled;

    public TableFieldColumnDefinition(Map<String, Object> data) {
        label = MapUtils.getString(data, "label");
        datatype = MapUtils.getEnum(data, "datatype", String.class, TableColumnDataType.values());
        placeholder = MapUtils.getString(data, "placeholder");
        decimalPlaces = MapUtils.getInteger(data, "decimalPlaces");
        optional = MapUtils.getBoolean(data, "optional");
        disabled = MapUtils.getBoolean(data, "disabled");
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public TableColumnDataType getDatatype() {
        return datatype;
    }

    public void setDatatype(TableColumnDataType datatype) {
        this.datatype = datatype;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public Integer getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(Integer decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public Boolean getOptional() {
        return optional;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }
}
