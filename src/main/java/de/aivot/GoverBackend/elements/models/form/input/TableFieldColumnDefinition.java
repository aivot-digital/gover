package de.aivot.GoverBackend.elements.models.form.input;

import de.aivot.GoverBackend.enums.TableColumnDataType;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;
import java.util.Objects;

public class TableFieldColumnDefinition {
    private String label;
    private TableColumnDataType datatype;
    private String placeholder;
    private Integer decimalPlaces;
    private Boolean optional;
    private Boolean disabled;

    public TableFieldColumnDefinition(Map<String, Object> data) {
        label = MapUtils.getString(data, "label");
        datatype = MapUtils.getEnum(data, "datatype", String.class, TableColumnDataType.class, TableColumnDataType.values());
        placeholder = MapUtils.getString(data, "placeholder");
        decimalPlaces = MapUtils.getInteger(data, "decimalPlaces");
        optional = MapUtils.getBoolean(data, "optional");
        disabled = MapUtils.getBoolean(data, "disabled");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableFieldColumnDefinition that = (TableFieldColumnDefinition) o;

        if (!Objects.equals(label, that.label)) return false;
        if (datatype != that.datatype) return false;
        if (!Objects.equals(placeholder, that.placeholder)) return false;
        if (!Objects.equals(decimalPlaces, that.decimalPlaces))
            return false;
        if (!Objects.equals(optional, that.optional)) return false;
        return Objects.equals(disabled, that.disabled);
    }

    @Override
    public int hashCode() {
        int result = label != null ? label.hashCode() : 0;
        result = 31 * result + (datatype != null ? datatype.hashCode() : 0);
        result = 31 * result + (placeholder != null ? placeholder.hashCode() : 0);
        result = 31 * result + (decimalPlaces != null ? decimalPlaces.hashCode() : 0);
        result = 31 * result + (optional != null ? optional.hashCode() : 0);
        result = 31 * result + (disabled != null ? disabled.hashCode() : 0);
        return result;
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
