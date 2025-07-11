package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class NumberField extends BaseInputElement<BigDecimal> {
    public static final BigDecimal AbsoluteMaxValue = BigDecimal.valueOf(Math.pow(2, 31));
    public static final BigDecimal AbsoluteMinValue = AbsoluteMaxValue.multiply(BigDecimal.valueOf(-1));
    public static final int DEFAULT_SCALE = 0;

    @Nullable
    private String placeholder;
    @Nullable
    private Integer decimalPlaces;
    @Nullable
    private String suffix;

    public NumberField() {
        super(ElementType.Number);
    }

    @Nullable
    @Override
    public BigDecimal formatValue(@Nullable Object value) {
        return _formatValue(value, decimalPlaces != null ? decimalPlaces : DEFAULT_SCALE);
    }

    @Override
    public void performValidation(@Nullable BigDecimal value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new ValidationException(this, "Bitte geben Sie einen Wert ein.");
            }
            return;
        }

        if (value.compareTo(AbsoluteMinValue) < 0) {
            var msg = String.format("Der Wert muss mindestens %s betragen.", formatGermanNumber(AbsoluteMinValue, 0));
            throw new ValidationException(this, msg);
        }

        if (value.compareTo(AbsoluteMaxValue) > 0) {
            var msg = String.format("Der Wert darf maximal %s betragen.", formatGermanNumber(AbsoluteMaxValue, 0));
            throw new ValidationException(this, msg);
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable BigDecimal value) {
        if (value == null) {
            return "Keine Angabe";
        }

        return formatGermanNumber(value, decimalPlaces != null ? decimalPlaces : DEFAULT_SCALE);
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        BigDecimal valA = formatValue(referencedValue);
        BigDecimal valB = formatValue(comparedValue);

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

    @Nullable
    public static BigDecimal _formatValue(Object value) {
        return _formatValue(value, DEFAULT_SCALE);
    }

    @Nullable
    public static BigDecimal _formatValue(Object value, int scale) {
        var res = switch (value) {
            case null -> null;
            case Integer iValue -> BigDecimal.valueOf(iValue);
            case Long lValue -> BigDecimal.valueOf(lValue);
            case Double dValue -> BigDecimal.valueOf(dValue);
            case Float fValue -> BigDecimal.valueOf(fValue);
            case BigDecimal bValue -> bValue;
            case BigInteger bValue -> BigDecimal.valueOf(bValue.doubleValue());
            case Number number -> BigDecimal.valueOf(number.doubleValue());
            case String sValue -> {
                try {
                    yield new BigDecimal(sValue);
                } catch (NumberFormatException ignored) {
                    try {
                        var iVal = new BigInteger(sValue);
                        yield BigDecimal.valueOf(iVal.doubleValue());
                    } catch (NumberFormatException ignored2) {
                        yield parseGermanNumber(sValue, scale);
                    }
                }
            }
            default -> null;
        };

        return res == null ? null : res.setScale(scale, RoundingMode.HALF_UP);
    }

    @Nonnull
    public static String formatGermanNumber(@Nonnull Number value) {
        return formatGermanNumber(value, DEFAULT_SCALE);
    }

    @Nonnull
    public static String formatGermanNumber(@Nonnull Number value, int scale) {
        Locale locale = Locale.GERMAN;
        NumberFormat formatter = NumberFormat.getNumberInstance(locale);
        formatter.setMinimumFractionDigits(scale);
        formatter.setMaximumFractionDigits(scale);
        return formatter.format(value);
    }

    @Nullable
    public static BigDecimal parseGermanNumber(@Nonnull String value) {
        return parseGermanNumber(value, DEFAULT_SCALE);
    }

    @Nullable
    public static BigDecimal parseGermanNumber(@Nonnull String value, int scale) {
        var normalizedValue = value
                .replaceAll("\\.", "")
                .replace(",", ".");

        try {
            return new BigDecimal(normalizedValue)
                    .setScale(scale, RoundingMode.HALF_UP);
        } catch (NumberFormatException exp) {
            try {
                var iVal = new BigInteger(normalizedValue);
                return BigDecimal
                        .valueOf(iVal.doubleValue())
                        .setScale(scale, RoundingMode.HALF_UP);
            } catch (NumberFormatException ignored2) {
                return null;
            }
        }
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NumberField that = (NumberField) o;
        return Objects.equals(placeholder, that.placeholder) && Objects.equals(decimalPlaces, that.decimalPlaces) && Objects.equals(suffix, that.suffix);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(placeholder);
        result = 31 * result + Objects.hashCode(decimalPlaces);
        result = 31 * result + Objects.hashCode(suffix);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public NumberField setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public Integer getDecimalPlaces() {
        return decimalPlaces;
    }

    public NumberField setDecimalPlaces(@Nullable Integer decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
        return this;
    }

    @Nullable
    public String getSuffix() {
        return suffix;
    }

    public NumberField setSuffix(@Nullable String suffix) {
        this.suffix = suffix;
        return this;
    }

    // endregion
}
