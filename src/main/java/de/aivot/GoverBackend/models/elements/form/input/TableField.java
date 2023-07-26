package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.enums.TableColumnDataType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.HeadlinePdfRowDto;
import de.aivot.GoverBackend.pdf.TablePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.script.ScriptEngine;
import java.util.*;

public class TableField extends BaseInputElement<Collection<Map<String, Object>>> {
    private Collection<TableFieldColumnDefinition> fields;
    private Integer maximumRows;
    private Integer minimumRequiredRows;

    public TableField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        fields = MapUtils.getCollection(values, "fields", TableFieldColumnDefinition::new);
        maximumRows = MapUtils.getInteger(values, "maximumRows");
        minimumRequiredRows = MapUtils.getInteger(values, "minimumRequiredRows");
    }

    @Override
    protected Collection<Map<String, Object>> formatValue(Object value) {
        Collection<Map<String, Object>> res = new LinkedList<>();

        if (value instanceof Collection<?> cValue) {
            for (Object o : cValue) {
                if (o instanceof Map<?,?> mValue) {
                    res.add((Map<String, Object>) mValue);
                }
            }
        }

        return res.isEmpty() ? null : res;
    }

    @Override
    public void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, Collection<Map<String, Object>> value, ScriptEngine scriptEngine) throws ValidationException {
        if (Boolean.TRUE.equals(getRequired()) && (value == null || value.isEmpty())) {
            throw new RequiredValidationException(this);
        }

        if (minimumRequiredRows != null && (value == null || value.size() < minimumRequiredRows)) {
            throw new ValidationException(this, "Not enough rows");
        }

        if (value == null || value.isEmpty()) {
            return;
        }

        if (maximumRows != null && value.size() > maximumRows) {
            throw new ValidationException(this, "Too many rows");
        }

        for (Map<String, Object> row : value) {
            for (TableFieldColumnDefinition col : fields) {
                Object val = row.get(col.getLabel());

                if (!Boolean.TRUE.equals(col.getOptional())) {
                    if (val == null) {
                        throw new ValidationException(this, "No value in required column " + col.getLabel());
                    } else {
                        if (val instanceof String sVal && sVal.trim().isEmpty()) {
                            throw new ValidationException(this, "No value in required column " + col.getLabel());
                        }
                    }
                }

                if (TableColumnDataType.Number == col.getDatatype()) {
                    if (!(val instanceof Integer || val instanceof Double || val instanceof Float || val instanceof Long || val instanceof Short)) {
                        throw new ValidationException(this, "Failed to parse number value in column " + col.getLabel() + " in table");
                    }
                }

                if (TableColumnDataType.String == col.getDatatype()) {
                    if (!(val instanceof String)) {
                        throw new ValidationException(this, "Failed to parse string value in column " + col.getLabel() + " in table");
                    }
                }
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, Collection<Map<String, Object>> value, String idPrefix, ScriptEngine scriptEngine) {
        List<String> columnHeaders = new LinkedList<>();

        for (TableFieldColumnDefinition col : fields) {
            columnHeaders.add(col.getLabel());
        }

        List<List<String>> columnValues = new LinkedList<>();

        if (value != null && !value.isEmpty()) {
            for (Map<String, Object> row : value) {
                List<String> fields = new LinkedList<>();
                for (TableFieldColumnDefinition col : this.fields) {
                    Object cellValue = row.get(col.getLabel());
                    TableColumnDataType colType = col.getDatatype();
                    if (colType == null) {
                        colType = TableColumnDataType.String;
                    }
                    switch (colType) {
                        case String -> {
                            if (cellValue instanceof String sCellValue) {
                                fields.add(sCellValue);
                            }
                        }
                        case Number -> {
                            if (cellValue instanceof Integer iCellValue) {
                                fields.add(String.format("%d", iCellValue));
                            } else if (cellValue instanceof Long lCellValue) {
                                fields.add(String.format("%d", lCellValue));
                            } else if (cellValue instanceof Double dCellValue) {
                                int decimalPlaces = col.getDecimalPlaces() == null ? col.getDecimalPlaces() : 2;
                                String decimalFormat = "%." + decimalPlaces + "f";
                                fields.add(String.format(decimalFormat, dCellValue));
                            } else if (cellValue instanceof Float fCellValue) {
                                int decimalPlaces = col.getDecimalPlaces() == null ? col.getDecimalPlaces() : 2;
                                String decimalFormat = "%." + decimalPlaces + "f";
                                fields.add(String.format(decimalFormat, fCellValue));
                            }
                        }
                    }
                }
                columnValues.add(fields);
            }
        } else {
            List<String> emptyRow = new LinkedList<>();
            for (TableFieldColumnDefinition col : this.fields) {
                emptyRow.add("Keine Angaben");
            }
            columnValues.add(emptyRow);
        }

        List<BasePdfRowDto> fields = new LinkedList<>();
        fields.add(new HeadlinePdfRowDto(getLabel(), 5));
        fields.add(new TablePdfRowDto(getLabel(), columnHeaders, columnValues));
        return fields;
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
}
