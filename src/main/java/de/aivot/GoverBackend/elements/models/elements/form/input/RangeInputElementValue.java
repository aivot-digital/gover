package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

public class RangeInputElementValue implements Serializable {
    @Nullable
    private ZonedDateTime start;

    @Nullable
    private ZonedDateTime end;

    public RangeInputElementValue() {
    }

    public RangeInputElementValue(@Nullable ZonedDateTime start, @Nullable ZonedDateTime end) {
        this.start = start;
        this.end = end;
    }

    public boolean isEmpty() {
        return start == null && end == null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RangeInputElementValue that = (RangeInputElementValue) o;
        return Objects.equals(start, that.start) && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Nullable
    public ZonedDateTime getStart() {
        return start;
    }

    public RangeInputElementValue setStart(@Nullable ZonedDateTime start) {
        this.start = start;
        return this;
    }

    @Nullable
    public ZonedDateTime getEnd() {
        return end;
    }

    public RangeInputElementValue setEnd(@Nullable ZonedDateTime end) {
        this.end = end;
        return this;
    }
}
