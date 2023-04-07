package de.aivot.GoverBackend.models.elements.form.input;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.enums.TableColumnDataType;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.InputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.TablePdfRowDto;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TableField extends InputElement<List<Map<String, String>>> {
    private Collection<TableFieldColumnDefinition> fields;
    private Integer maximumRows;
    private Integer minimumRequiredRows;

    public TableField(BaseElement parent, Map<String, Object> data) {
        super(data);
        // TODO
    }

    @Nullable
    public Collection<TableFieldColumnDefinition> getFields() {
        return fields;
    }

    public void setFields(Collection<TableFieldColumnDefinition> fields) {
        this.fields = fields;
    }

    @Nullable
    public Integer getMaximumRows() {
        return maximumRows;
    }

    public void setMaximumRows(Integer maximumRows) {
        this.maximumRows = maximumRows;
    }

    @Nullable
    public Integer getMinimumRequiredRows() {
        return minimumRequiredRows;
    }

    public void setMinimumRequiredRows(Integer minimumRequiredRows) {
        this.minimumRequiredRows = minimumRequiredRows;
    }

    @Override
    public boolean isValid(List<Map<String, String>> value, String idPrefix) {
        if (Boolean.TRUE.equals(getRequired()) && value.isEmpty()) {
            return false;
        }

        if (minimumRequiredRows != null && value.size() < minimumRequiredRows) {
            return false;
        }

        if (maximumRows != null && value.size() > maximumRows) {
            return false;
        }

        for (Map<String, String> row : value) {
            for (TableFieldColumnDefinition col : fields) {
                String val = row.get(col.getLabel());

                if (!Boolean.TRUE.equals(col.getOptional())) {
                    if (val == null || val.trim().isEmpty()) {
                        return false;
                    }
                }

                if (TableColumnDataType.Number == col.getDatatype()) {
                    try {
                        Double.parseDouble(val);
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(List<Map<String, String>> value, String idPrefix) {
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
