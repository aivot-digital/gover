package de.aivot.prosuna.backend.elements.models.elements.form.input;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Holds range boundaries without imposing one temporal representation on every range field.
 * The owning input element supplies the concrete boundary type.
 */
public class RangeInputElementValue<T> implements Serializable {
    @Nullable
    private T start;

    @Nullable
    private T end;

    public RangeInputElementValue() {
    }

    public RangeInputElementValue(@Nullable T start, @Nullable T end) {
        this.start = start;
        this.end = end;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return start == null && end == null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RangeInputElementValue<?> that = (RangeInputElementValue<?>) o;
        return Objects.equals(start, that.start) && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Nullable
    public T getStart() {
        return start;
    }

    public RangeInputElementValue<T> setStart(@Nullable T start) {
        this.start = start;
        return this;
    }

    @Nullable
    public T getEnd() {
        return end;
    }

    public RangeInputElementValue<T> setEnd(@Nullable T end) {
        this.end = end;
        return this;
    }
}
