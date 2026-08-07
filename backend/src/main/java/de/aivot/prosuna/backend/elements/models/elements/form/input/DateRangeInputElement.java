package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.PrintableElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.DateType;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.RequiredValidationException;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.temporal.TemporalAccessor;
import java.util.Map;
import java.util.Objects;

public class DateRangeInputElement extends BaseInputElement<RangeInputElementValue<TemporalAccessor>> implements PrintableElement<RangeInputElementValue<TemporalAccessor>> {
    @Nullable
    private String placeholder;

    @Nullable
    private DateType mode;

    public DateRangeInputElement() {
        super(ElementType.DateRange);
    }

    @Nullable
    @Override
    public RangeInputElementValue<TemporalAccessor> formatValue(@Nullable Object value) {
        return _formatValue(value, mode);
    }

    @Override
    public void performValidation(@Nullable RangeInputElementValue<TemporalAccessor> value) throws ValidationException {
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

        var start = DateInputElement.toLocalDate(value.getStart());
        var end = DateInputElement.toLocalDate(value.getEnd());
        if (start != null && end != null && start.isAfter(end)) {
            throw new ValidationException(this, "Der Startwert darf nicht größer als der Endwert sein.");
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable RangeInputElementValue<TemporalAccessor> value) {
        if (value == null || value.isEmpty()) {
            return "Keine Angabe";
        }

        var formatter = new DateInputElement().setMode(mode);
        var start = formatter.toDisplayValue(value.getStart());
        var end = formatter.toDisplayValue(value.getEnd());
        return start + " bis " + end;
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        var valA = _formatValue(referencedValue, mode);
        if (valA == null || valA.isEmpty()) {
            return operator == ConditionOperator.Empty;
        }

        if (operator == ConditionOperator.NotEmpty) {
            return true;
        }

        var valB = _formatValue(comparedValue, mode);
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
    public static RangeInputElementValue<TemporalAccessor> _formatValue(
            @Nullable Object value,
            @Nullable DateType mode
    ) {
        var formatter = new DateInputElement().setMode(mode);

        switch (value) {
            case null:
                return null;
            case RangeInputElementValue<?> val:
                var normalizedRange = new RangeInputElementValue<TemporalAccessor>(
                        formatter.formatValue(val.getStart()),
                        formatter.formatValue(val.getEnd())
                );
                return normalizedRange.isEmpty() ? null : normalizedRange;
            case Map<?, ?> map:
                var start = formatter.formatValue(map.get("start"));
                var end = formatter.formatValue(map.get("end"));
                var range = new RangeInputElementValue<TemporalAccessor>(start, end);
                return range.isEmpty() ? null : range;
            default:
                return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DateRangeInputElement that = (DateRangeInputElement) o;
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

    public DateRangeInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public DateType getMode() {
        return mode;
    }

    public DateRangeInputElement setMode(@Nullable DateType mode) {
        this.mode = mode;
        return this;
    }
}
