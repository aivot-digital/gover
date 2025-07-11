package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.enums.TableColumnDataType;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class TableFieldColumnDefinition {
    @Nullable
    private String key;
    @Nullable
    private String label;
    @Nullable
    private TableColumnDataType datatype;
    @Nullable
    private String placeholder;
    @Nullable
    private Integer decimalPlaces;
    @Nullable
    private Boolean optional;
    @Nullable
    private Boolean disabled;

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        TableFieldColumnDefinition that = (TableFieldColumnDefinition) o;
        return Objects.equals(key, that.key) && Objects.equals(label, that.label) && datatype == that.datatype && Objects.equals(placeholder, that.placeholder) && Objects.equals(decimalPlaces, that.decimalPlaces) && Objects.equals(optional, that.optional) && Objects.equals(disabled, that.disabled);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(key);
        result = 31 * result + Objects.hashCode(label);
        result = 31 * result + Objects.hashCode(datatype);
        result = 31 * result + Objects.hashCode(placeholder);
        result = 31 * result + Objects.hashCode(decimalPlaces);
        result = 31 * result + Objects.hashCode(optional);
        result = 31 * result + Objects.hashCode(disabled);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getKey() {
        return key;
    }

    public TableFieldColumnDefinition setKey(@Nullable String key) {
        this.key = key;
        return this;
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    public TableFieldColumnDefinition setLabel(@Nullable String label) {
        this.label = label;
        return this;
    }

    @Nullable
    public TableColumnDataType getDatatype() {
        return datatype;
    }

    public TableFieldColumnDefinition setDatatype(@Nullable TableColumnDataType datatype) {
        this.datatype = datatype;
        return this;
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public TableFieldColumnDefinition setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public Integer getDecimalPlaces() {
        return decimalPlaces;
    }

    public TableFieldColumnDefinition setDecimalPlaces(@Nullable Integer decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
        return this;
    }

    @Nullable
    public Boolean getOptional() {
        return optional;
    }

    public TableFieldColumnDefinition setOptional(@Nullable Boolean optional) {
        this.optional = optional;
        return this;
    }

    @Nullable
    public Boolean getDisabled() {
        return disabled;
    }

    public TableFieldColumnDefinition setDisabled(@Nullable Boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    // endregion
}
