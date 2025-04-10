package de.aivot.GoverBackend.elements.models.form.input;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.models.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.*;

public class RadioField extends BaseInputElement<String> {
    private Collection<Object> options;
    private Boolean displayInline;

    public RadioField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        options = MapUtils.getObjectCollection(values, "options");
        displayInline = MapUtils.getBoolean(values, "displayInline");
    }

    @Override
    public String formatValue(Object value) {
        if (value instanceof String sValue) {
            return sValue;
        }
        return null;
    }

    @Override
    public void validate(String value) throws ValidationException {
        testValueInOptions(value);
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, String value, String idPrefix, FormState formState) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        var renderData = "Keine Angabe";
        if (value != null) {
            for (var opt : options) {
                if (opt instanceof String sOpt) {
                    if (sOpt.equals(value)) {
                        renderData = sOpt;
                        break;
                    }
                } else if (opt instanceof Map<?,?> mOpt) {
                    var optValue = mOpt.get("value");
                    if (optValue != null && optValue.equals(value)) {
                        renderData = (String) mOpt.get("label");
                        break;
                    }
                }
            }
        }

        fields.add(new ValuePdfRowDto(
                getLabel(),
                renderData,
                this
        ));

        return fields;
    }

    private void testValueInOptions(String value) throws ValidationException {
        boolean valueFound = false;
        for (Object opt : options) {
            if (opt instanceof String sOpt) {
                if (sOpt.equals(value)) {
                    valueFound = true;
                    break;
                }
            } else if (opt instanceof Map<?,?> mOpt) {
                var optValue = mOpt.get("value");
                if (optValue != null && optValue.equals(value)) {
                    valueFound = true;
                    break;
                }
            }
        }
        if (!valueFound) {
            throw new ValidationException(this, "Ungültige Auswahl: " + value);
        }
    }

    @Override
    public boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        if (referencedValue == null) {
            return switch (operator) {
                case Equals -> comparedValue == null;
                case NotEquals -> comparedValue != null;
                case Empty -> true;
                default -> false;
            };
        }

        if (operator == ConditionOperator.NotEmpty) {
            return true;
        }

        if (referencedValue instanceof String sValA && comparedValue instanceof String sValB) {
            return switch (operator) {
                case Equals -> sValA.equals(sValB);
                case NotEquals -> !sValA.equals(sValB);

                default -> false;
            };
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RadioField that = (RadioField) o;

        if (!Objects.equals(options, that.options)) return false;
        return Objects.equals(displayInline, that.displayInline);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (options != null ? options.hashCode() : 0);
        result = 31 * result + (displayInline != null ? displayInline.hashCode() : 0);
        return result;
    }

    public Collection<Object> getOptions() {
        return options;
    }

    public void setOptions(Collection<Object> options) {
        this.options = options;
    }

    public Boolean getDisplayInline() {
        return displayInline;
    }

    public RadioField setDisplayInline(Boolean displayInline) {
        this.displayInline = displayInline;
        return this;
    }
}
