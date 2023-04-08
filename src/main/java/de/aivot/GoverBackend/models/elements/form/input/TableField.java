package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.enums.TableColumnDataType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.form.InputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.TablePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TableField extends InputElement<List<Map<String, String>>> {
    private Collection<TableFieldColumnDefinition> fields;
    private Integer maximumRows;
    private Integer minimumRequiredRows;

    public TableField(Map<String, Object> data) {
        super(data);

        fields = MapUtils.getCollection(data, "fields", TableFieldColumnDefinition::new);
        maximumRows = MapUtils.getInteger(data, "maximumRows");
        minimumRequiredRows = MapUtils.getInteger(data, "minimumRequiredRows");
    }

    public Collection<TableFieldColumnDefinition> getFields() {
        return fields;
    }

    public void setFields(Collection<TableFieldColumnDefinition> fields) {
        this.fields = fields;
    }

    public Integer getMaximumRows() {
        return maximumRows;
    }

    public void setMaximumRows(Integer maximumRows) {
        this.maximumRows = maximumRows;
    }

    public Integer getMinimumRequiredRows() {
        return minimumRequiredRows;
    }

    public void setMinimumRequiredRows(Integer minimumRequiredRows) {
        this.minimumRequiredRows = minimumRequiredRows;
    }

    @Override
    public void validate(Map<String, Object> customerInput, List<Map<String, String>> value, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        if (Boolean.TRUE.equals(getRequired()) && value.isEmpty()) {
            throw new RequiredValidationException(this);
        }

        if (minimumRequiredRows != null && value.size() < minimumRequiredRows) {
            throw new ValidationException(this, "Not enough rows");
        }

        if (maximumRows != null && value.size() > maximumRows) {
            throw new ValidationException(this, "Too many rows");
        }

        for (Map<String, String> row : value) {
            for (TableFieldColumnDefinition col : fields) {
                String val = row.get(col.getLabel());

                if (!Boolean.TRUE.equals(col.getOptional())) {
                    if (val == null || val.trim().isEmpty()) {
                        throw new ValidationException(this, "No value in required column " + col.getLabel());
                    }
                }

                if (TableColumnDataType.Number == col.getDatatype()) {
                    try {
                        Double.parseDouble(val);
                    } catch (NumberFormatException e) {
                        throw new ValidationException(this, "Failed to parse number value in column " + col.getLabel() + " in table: " + e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, List<Map<String, String>> value, String idPrefix, ScriptEngine scriptEngine) {
        List<String> columnHeaders = new LinkedList<>();

        for (TableFieldColumnDefinition col : fields) {
            columnHeaders.add(col.getLabel());
        }

        List<List<String>> columnValues = new LinkedList<>();

        if (value != null) {
            for (Map<String, String> row : value) {
                List<String> fields = new LinkedList<>();
                for (TableFieldColumnDefinition col : this.fields) {
                    String cellValue = row.get(col.getLabel());

                    switch (col.getDatatype()) {
                        case String -> {
                            fields.add(cellValue);
                        }
                        case Number -> {
                            int decimalPlaces = col.getDecimalPlaces() == null ? col.getDecimalPlaces() : 2;
                            String decimalFormat = "%." + decimalPlaces + "f";

                            try {
                                Double val = Double.parseDouble((String) cellValue);
                                fields.add(String.format(decimalFormat, val));
                            } catch (Exception e) {
                                fields.add((String) cellValue);
                            }

                        }
                    }
                }
                columnValues.add(fields);
            }
        }

        List<BasePdfRowDto> fields = new LinkedList<>();
        fields.add(new TablePdfRowDto(getLabel(), columnHeaders, columnValues));
        return fields;
    }
}
