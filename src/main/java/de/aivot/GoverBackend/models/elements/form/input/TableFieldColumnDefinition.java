package de.aivot.GoverBackend.models.elements.form.input;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.enums.TableColumnDataType;

public class TableFieldColumnDefinition {
    private String label;
    private TableColumnDataType datatype;
    private String placeholder;
    private Integer decimalPlaces;
    private Boolean optional;
    private Boolean disabled;

    @Nullable
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Nullable
    public TableColumnDataType getDatatype() {
        return datatype;
    }

    public void setDatatype(TableColumnDataType datatype) {
        this.datatype = datatype;
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
    public Boolean getOptional() {
        return optional;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

    @Nullable
    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }
}
