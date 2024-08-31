package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.models.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;

public class NumberField extends BaseInputElement<Double> {
    private static final Logger logger = LoggerFactory.getLogger(NumberField.class);

    private String placeholder;
    private Integer decimalPlaces;
    private String suffix;

    public NumberField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        placeholder = MapUtils.getString(values, "placeholder");
        decimalPlaces = MapUtils.getInteger(values, "decimalPlaces");
        suffix = MapUtils.getString(values, "suffix");
    }

    @Override
    public Double formatValue(Object value) {
        if (value instanceof Integer iValue) {
            return iValue.doubleValue();
        }
        if (value instanceof Double dValue) {
            return dValue;
        }
        if (value instanceof Float fValue) {
            return fValue.doubleValue();
        }
        if (value instanceof BigDecimal bValue) {
            return bValue.doubleValue();
        }
        if (value instanceof String bValue) {
            try {
                return Double.valueOf(bValue);
            } catch (NumberFormatException exp) {
                logger.warn("Could not parse value to double: " + bValue, exp);

                try {
                    var iVal = Integer.parseInt(bValue);
                    return (double) iVal;
                } catch (NumberFormatException exp2) {
                    logger.warn("Could not parse value to integer: " + bValue, exp2);
                }
            }
        }
        return null;
    }

    @Override
    public void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, Double value, ScriptEngine scriptEngine) throws ValidationException {
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, Double value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        String displayValue = "Keine Angabe";

        if (value != null) {
            int decimalPlaces = this.decimalPlaces != null ? this.decimalPlaces : 0;

            Locale locale = Locale.GERMAN;
            NumberFormat formatter = NumberFormat.getNumberInstance(locale);
            formatter.setMinimumFractionDigits(decimalPlaces);
            formatter.setMaximumFractionDigits(decimalPlaces);
            displayValue = formatter.format(value);

            if (suffix != null) {
                displayValue += " " + getSuffix();
            }
        }

        fields.add(new ValuePdfRowDto(
                getLabel(),
                displayValue,
                this
        ));

        return fields;
    }

    public String toDisplayValue(Object rawValue) {
        var value = formatValue(rawValue);

        String displayValue = "Keine Angabe";

        if (value != null) {
            int decimalPlaces = this.decimalPlaces != null ? this.decimalPlaces : 0;

            Locale locale = Locale.GERMAN;
            NumberFormat formatter = NumberFormat.getNumberInstance(locale);
            formatter.setMinimumFractionDigits(decimalPlaces);
            formatter.setMaximumFractionDigits(decimalPlaces);
            displayValue = formatter.format(value);
        }

        return displayValue;
    }

    @Override
    public boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        BigDecimal valA = transformValue(referencedValue);
        BigDecimal valB = transformValue(comparedValue);

        return switch (operator) {
            case Equals -> (valA == null && valB == null) || (valA != null && valA.equals(valB));
            case NotEquals -> (valA == null && valB != null) || !(valA != null && valA.equals(valB));

            case LessThan -> valA != null && valB != null && valA.compareTo(valB) < 0;
            case LessThanOrEqual -> valA != null && valB != null && (valA.compareTo(valB) <= 0);

            case GreaterThan -> valA != null && valB != null && valA.compareTo(valB) > 0;
            case GreaterThanOrEqual -> valA != null && valB != null && valA.compareTo(valB) >= 0;

            case Empty -> valA == null;
            case NotEmpty -> valA != null;

            default -> false;
        };
    }

    /**
     * TODO: This method doubles with formatValue. It should be refactored to use the same method.
     */
    private BigDecimal transformValue(Object value) {
        if (value instanceof BigDecimal dValue) {
            return dValue;
        }

        if (value instanceof Double dValue) {
            return BigDecimal.valueOf(dValue);
        }

        if (value instanceof Integer iValue) {
            return BigDecimal.valueOf(iValue.doubleValue());
        }

        if (value instanceof String sValue) {
            Pattern defNumberPattern = Pattern.compile("^[1-9][0-9]*?(\\.[0-9]+)$");

            String sValueNormalized = null;
            if (defNumberPattern.matcher(sValue).matches()) {
                sValueNormalized = sValue;
            } else {
                sValueNormalized = sValue
                        .replaceAll("\\.", "")
                        .replace(",", ".");
            }

            try {
                return BigDecimal.valueOf(Double.parseDouble(sValueNormalized));
            } catch (NumberFormatException exp) {
                return null;
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NumberField that = (NumberField) o;

        if (!Objects.equals(placeholder, that.placeholder)) return false;
        if (!Objects.equals(decimalPlaces, that.decimalPlaces))
            return false;
        return Objects.equals(suffix, that.suffix);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (placeholder != null ? placeholder.hashCode() : 0);
        result = 31 * result + (decimalPlaces != null ? decimalPlaces.hashCode() : 0);
        result = 31 * result + (suffix != null ? suffix.hashCode() : 0);
        return result;
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

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}
