package de.aivot.GoverBackend.elements.models.elements.steps;

import de.aivot.GoverBackend.elements.models.elements.*;
import de.aivot.GoverBackend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class SummaryStepElement extends BaseStepElement implements InputElement<Boolean>, PrintableElement<Boolean> {
    public SummaryStepElement() {
        super(ElementType.SummaryStep);
    }

    @Override
    public Boolean getRequired() {
        return true;
    }

    @Override
    public void performValidation(Boolean value) throws ValidationException {
        if (!Boolean.TRUE.equals(value)) {
            throw new ValidationException(this, "Bitte prüfen und bestätigen Sie die Zusammenfassung.");
        }
    }

    @Override
    public Boolean formatValue(Object value) {
        return CheckboxInputElement._formatValue(value);
    }

    @Nullable
    @Override
    public ElementValueFunctions getValue() {
        return null;
    }

    @Nullable
    @Override
    public ElementValidationFunctions getValidation() {
        return null;
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable Boolean value) {
        return value == null || !value ? "Nicht bestätigt" : "Bestätigt";
    }
}
