package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;

public class NumberField extends BaseInputElement<Double> {
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
    protected Double formatValue(Object value) {
        if (value instanceof Integer iValue) {
            return Double.valueOf(iValue.doubleValue());
        }
        if (value instanceof Double dValue) {
            return dValue;
        }
        if (value instanceof Float fValue) {
            return Double.valueOf(fValue.doubleValue());
        }
        if (value instanceof BigDecimal bValue) {
            return Double.valueOf(bValue.doubleValue());
        }
        return null;
    }

    @Override
    public void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, Double value, ScriptEngine scriptEngine) throws ValidationException {
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, Double value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        String displayValue = "Keine Angaben";

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
                displayValue
        ));

        return fields;
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
