package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.enums.TimeType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

public class DateTimeRangeInputElement extends BaseInputElement<RangeInputElementValue> implements PrintableElement<RangeInputElementValue> {
    @Nullable
    private String placeholder;

    @Nullable
    private TimeType mode;

    public DateTimeRangeInputElement() {
        super(ElementType.DateTimeRange);
    }

    @Nullable
    @Override
    public RangeInputElementValue formatValue(@Nullable Object value) {
        return _formatValue(value);
    }

    @Override
    public void performValidation(@Nullable RangeInputElementValue value) throws ValidationException {
        if (value == null || value.isEmpty()) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
            return;
        }

        if (Boolean.TRUE.equals(getRequired())) {
            if (value.getStart() == null || value.getEnd() == null) {
                throw new RequiredValidationException(this);
            }
        }

        if ((value.getStart() == null) != (value.getEnd() == null)) {
            throw new ValidationException(this, "Bitte geben Sie sowohl den Start- als auch den Endwert an.");
        }

        if (value.getStart() != null && value.getEnd() != null && value.getStart().isAfter(value.getEnd())) {
            throw new ValidationException(this, "Der Startwert darf nicht größer als der Endwert sein.");
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable RangeInputElementValue value) {
        if (value == null || value.isEmpty()) {
            return "Keine Angabe";
        }

        var formatter = DateTimeFormatter
                .ofPattern(mode == TimeType.Second ? "dd.MM.yyyy HH:mm:ss" : "dd.MM.yyyy HH:mm")
                .withZone(DateInputElement.zoneId);

        var start = value.getStart() == null ? "Keine Angabe" : value.getStart().format(formatter) + " Uhr";
        var end = value.getEnd() == null ? "Keine Angabe" : value.getEnd().format(formatter) + " Uhr";
        return start + " bis " + end;
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        var valA = _formatValue(referencedValue);
        if (valA == null || valA.isEmpty()) {
            return operator == ConditionOperator.Empty;
        }

        if (operator == ConditionOperator.NotEmpty) {
            return true;
        }

        var valB = _formatValue(comparedValue);
        if (valB == null) {
            return false;
        }

        return switch (operator) {
            case Equals -> Objects.equals(valA, valB);
            case NotEquals -> !Objects.equals(valA, valB);
            default -> false;
        };
    }

    @Nullable
    public static RangeInputElementValue _formatValue(@Nullable Object value) {
        switch (value) {
            case null:
                return null;
            case RangeInputElementValue val:
                return val.isEmpty() ? null : val;
            case Map<?, ?> map:
                var start = DateTimeInputElement._formatValue(map.get("start"));
                var end = DateTimeInputElement._formatValue(map.get("end"));
                var range = new RangeInputElementValue(start, end);
                return range.isEmpty() ? null : range;
            default:
                return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DateTimeRangeInputElement that = (DateTimeRangeInputElement) o;
        return Objects.equals(placeholder, that.placeholder) && mode == that.mode;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(placeholder);
        result = 31 * result + Objects.hashCode(mode);
        return result;
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public DateTimeRangeInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public TimeType getMode() {
        return mode;
    }

    public DateTimeRangeInputElement setMode(@Nullable TimeType mode) {
        this.mode = mode;
        return this;
    }
}
