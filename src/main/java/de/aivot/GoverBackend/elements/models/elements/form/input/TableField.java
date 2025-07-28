package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.enums.TableColumnDataType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.util.*;

public class TableField extends BaseInputElement<List<Map<String, Object>>> {
    @Nullable
    private List<TableFieldColumnDefinition> fields;
    @Nullable
    private Integer maximumRows;
    @Nullable
    private Integer minimumRequiredRows;

    public TableField() {
        super(ElementType.Table);
    }

    @Override
    public List<Map<String, Object>> formatValue(Object value) {
        if (fields == null) {
            throw new IllegalStateException("TableField must have at least one column defined.");
        }

        List<Map<String, Object>> res = new LinkedList<>();

        if (value instanceof Collection<?> cValue) {
            for (Object o : cValue) {
                if (o instanceof Map<?, ?> mValue) {
                    for (var field : fields) {
                        var key = field.getKey();
                    }

                    var row = new HashMap<String, Object>();

                    for (var entry : mValue.entrySet()) {
                        if (entry.getKey() instanceof String sValue) {
                            row.put(sValue, entry.getValue());
                        }
                    }

                    res.add(row);
                }
            }
        }

        return res.isEmpty() ? null : res;
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable List<Map<String, Object>> value) {
        if (value == null || value.isEmpty()) {
            return "Keine Zeilen hinzugefügt";
        }

        if (fields == null || fields.isEmpty()) {
            return "Keine Spalten definiert";
        }

        var sb = new StringBuilder();

        for (var column : fields) {
            sb.append(column.getLabel());
            sb.append(',');
        }
        sb.append('\n');

        for (var row : value) {
            for (var col : fields) {
                var cellValue = row.get(col.getKey());
                sb.append(cellValue.toString());
                sb.append(',');
            }
            sb.append('\n');
        }

        return sb.toString();
    }

    @Override
    public void performValidation(@Nullable List<Map<String, Object>> value) throws ValidationException {
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
                Object val = row.get(col.getKey());

                if (val instanceof String sVal) {
                    if (StringUtils.isNullOrEmpty(sVal)) {
                        val = null;
                    }
                }

                if (!Boolean.TRUE.equals(col.getOptional())) {
                    if (val == null) {
                        var msg = String.format(
                                "In Spalte \"%s\" in Zeile %d wurde kein Wert angegeben. Diese Spalte ist jedoch eine Pflichtangabe.",
                                col.getLabel(),
                                rowNumber
                        );

                        throw new ValidationException(this, msg);
                    } else {
                        if (val instanceof String sVal && sVal.trim().isEmpty()) {
                            var msg = String.format(
                                    "In Spalte \"%s\" in Zeile %d wurde kein Wert angegeben. Diese Spalte ist jedoch eine Pflichtangabe.",
                                    col.getLabel(),
                                    rowNumber
                            );

                            throw new ValidationException(this, msg);
                        }
                    }
                }

                if (TableColumnDataType.Number == col.getDatatype()) {
                    if (val == null) {
                        // Do nothing, as null is allowed
                    } else if (val instanceof Number nValue) {
                        var dValue = BigDecimal.valueOf(nValue.doubleValue());

                        if (dValue.compareTo(NumberField.AbsoluteMinValue) < 0) {
                            var msg = String.format(
                                    "Der Wert in Spalte \"%s\" in Zeile %d muss mindestens %s betragen.",
                                    col.getLabel(),
                                    rowNumber,
                                    NumberField.formatGermanNumber(NumberField.AbsoluteMinValue, 0)
                            );
                            throw new ValidationException(this, msg);
                        }

                        if (dValue.compareTo(NumberField.AbsoluteMaxValue) > 0) {
                            var msg = String.format(
                                    "Der Wert in Spalte %s in Zeile %d darf maximal %s betragen.",
                                    col.getLabel(),
                                    rowNumber,
                                    NumberField.formatGermanNumber(NumberField.AbsoluteMaxValue, 0)
                            );
                            throw new ValidationException(this, msg);
                        }
                    } else {
                        throw new ValidationException(this, "Der Wert in Spalte \"" + col.getLabel() + "\" der Zeile " + rowNumber + " konnte nicht als Zahl interpretiert werden.");
                    }
                }

                if (TableColumnDataType.String == col.getDatatype()) {
                    if (val == null) {
                        // Do nothing, as null is allowed
                    } else if (!(val instanceof String)) {
                        throw new ValidationException(this, "Der Wert in Spalte \"" + col.getLabel() + "\" der Zeile " + rowNumber + " konnte nicht als Text interpretiert werden.");
                    }
                }
            }
            rowNumber++;
        }
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TableField that = (TableField) o;
        return Objects.equals(fields, that.fields) && Objects.equals(maximumRows, that.maximumRows) && Objects.equals(minimumRequiredRows, that.minimumRequiredRows);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(fields);
        result = 31 * result + Objects.hashCode(maximumRows);
        result = 31 * result + Objects.hashCode(minimumRequiredRows);
        return result;
    }

    // endregion

    // region Getters & Setters

    public List<TableFieldColumnDefinition> getFields() {
        return fields;
    }

    public TableField setFields(List<TableFieldColumnDefinition> fields) {
        this.fields = fields;
        return this;
    }

    public Integer getMaximumRows() {
        return maximumRows;
    }

    public TableField setMaximumRows(Integer maximumRows) {
        this.maximumRows = maximumRows;
        return this;
    }

    public Integer getMinimumRequiredRows() {
        return minimumRequiredRows;
    }

    public TableField setMinimumRequiredRows(Integer minimumRequiredRows) {
        this.minimumRequiredRows = minimumRequiredRows;
        return this;
    }

    // endregion
}
