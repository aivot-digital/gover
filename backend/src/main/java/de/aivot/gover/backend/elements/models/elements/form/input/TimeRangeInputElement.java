package de.aivot.gover.backend.elements.models.elements.form.input;

import de.aivot.gover.backend.elements.models.elements.BaseInputElement;
import de.aivot.gover.backend.elements.models.elements.PrintableElement;
import de.aivot.gover.backend.enums.ConditionOperator;
import de.aivot.gover.backend.enums.ElementType;
import de.aivot.gover.backend.enums.TimeType;
import de.aivot.gover.backend.exceptions.RequiredValidationException;
import de.aivot.gover.backend.exceptions.ValidationException;
import de.aivot.gover.backend.utils.ApplicationTimeZone;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

public class TimeRangeInputElement extends BaseInputElement<RangeInputElementValue> implements PrintableElement<RangeInputElementValue> {
    @Nullable
    private TimeType mode;

    public TimeRangeInputElement() {
        super(ElementType.TimeRange);
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
                .ofPattern(mode == TimeType.Second ? "HH:mm:ss" : "HH:mm")
                .withZone(ApplicationTimeZone.getZoneId());

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
                var formatter = new TimeInputElement();
                var start = formatter.formatValue(map.get("start"));
                var end = formatter.formatValue(map.get("end"));
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
        TimeRangeInputElement that = (TimeRangeInputElement) o;
        return mode == that.mode;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(mode);
        return result;
    }

    @Nullable
    public TimeType getMode() {
        return mode;
    }

    public TimeRangeInputElement setMode(@Nullable TimeType mode) {
        this.mode = mode;
        return this;
    }
}
