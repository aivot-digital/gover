package de.aivot.GoverBackend.elements.models.form.input;

import de.aivot.GoverBackend.enums.TableColumnDataType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.models.pdf.HeadlinePdfRowDto;
import de.aivot.GoverBackend.models.pdf.TablePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

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
    public Collection<Map<String, Object>> formatValue(Object value) {
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
    public void validate(Collection<Map<String, Object>> value) throws ValidationException {
        if (Boolean.TRUE.equals(getRequired()) && (value == null || value.isEmpty())) {
            throw new RequiredValidationException(this);
        }

        if (minimumRequiredRows != null && (value == null || value.size() < minimumRequiredRows)) {
            int actualRows = value == null ? 0 : value.size();
            String actualLabel = actualRows == 1 ? "Zeile" : "Zeilen";
            String minLabel = minimumRequiredRows == 1 ? "Zeile" : "Zeilen";

            throw new ValidationException(this, "Es müssen mindestens " + minimumRequiredRows + " " + minLabel + " hinzugefügt werden (derzeit nur " + actualRows + " " + actualLabel + ").");
        }

        if (value == null || value.isEmpty()) {
            return;
        }

        if (maximumRows != null && value.size() > maximumRows) {
            String maxLabel = maximumRows == 1 ? "Zeile" : "Zeilen";
            String actualLabel = value.size() == 1 ? "Zeile" : "Zeilen";

            throw new ValidationException(this, "Es dürfen maximal " + maximumRows + " " + maxLabel + " hinzugefügt werden (derzeit " + value.size() + " " + actualLabel + ").");
        }

        int rowNumber = 1;
        for (Map<String, Object> row : value) {
            for (TableFieldColumnDefinition col : fields) {
                Object val = row.get(col.getLabel());

                if (!Boolean.TRUE.equals(col.getOptional())) {
                    if (val == null) {
                        throw new ValidationException(this, "In Spalte " + col.getLabel() + " der Zeile " + rowNumber + " wurde kein Wert angegeben. Diese Spalte ist jedoch eine Pflichtangabe.");
                    } else {
                        if (val instanceof String sVal && sVal.trim().isEmpty()) {
                            throw new ValidationException(this, "In Spalte " + col.getLabel() + " der Zeile " + rowNumber + " wurde kein Wert angegeben. Diese Spalte ist jedoch eine Pflichtangabe.");
                        }
                    }
                }

                if (TableColumnDataType.Number == col.getDatatype()) {
                    if (val instanceof Number nValue) {
                        var dValue = nValue.doubleValue();

                        if (dValue < NumberField.AbsoluteMinValue) {
                            var msg = String.format(
                                    "Der Wert in Spalte %s in Zeile %d muss mindestens %s betragen.",
                                    col.getLabel(),
                                    rowNumber,
                                    NumberField.formatGermanNumber(NumberField.AbsoluteMinValue, 0)
                            );
                            throw new ValidationException(this, msg);
                        }

                        if (dValue > NumberField.AbsoluteMaxValue) {
                            var msg = String.format(
                                    "Der Wert in Spalte %s in Zeile %d darf maximal %s betragen.",
                                    col.getLabel(),
                                    rowNumber,
                                    NumberField.formatGermanNumber(NumberField.AbsoluteMaxValue, 0)
                            );
                            throw new ValidationException(this, msg);
                        }
                    } else {
                        throw new ValidationException(this, "Der Wert in Spalte " + col.getLabel() + " der Zeile " + rowNumber + " konnte nicht als Zahl interpretiert werden.");
                    }
                }

                if (TableColumnDataType.String == col.getDatatype()) {
                    if (!(val instanceof String)) {
                        throw new ValidationException(this, "Der Wert in Spalte " + col.getLabel() + " der Zeile " + rowNumber + " konnte nicht als Text interpretiert werden.");
                    }
                }
            }
            rowNumber++;
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Collection<Map<String, Object>> value, String idPrefix, FormState formState) {
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
                            } else {
                                fields.add("Keine Angaben");
                            }
                        }
                        case Number -> {
                            if (cellValue instanceof Number nCellValue) {
                                var dCellValue = nCellValue.doubleValue();
                                var formatted = NumberField.formatGermanNumber(dCellValue, col.getDecimalPlaces() != null ? col.getDecimalPlaces() : 0);
                                fields.add(formatted);
                            } else {
                                fields.add("Keine Angaben");
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
        fields.add(new HeadlinePdfRowDto(getLabel(), 5, this));
        fields.add(new TablePdfRowDto(getLabel(), columnHeaders, columnValues, this));
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TableField that = (TableField) o;

        if (!Objects.equals(fields, that.fields)) return false;
        if (!Objects.equals(maximumRows, that.maximumRows)) return false;
        return Objects.equals(minimumRequiredRows, that.minimumRequiredRows);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        result = 31 * result + (maximumRows != null ? maximumRows.hashCode() : 0);
        result = 31 * result + (minimumRequiredRows != null ? minimumRequiredRows.hashCode() : 0);
        return result;
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
