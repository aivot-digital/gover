package de.aivot.GoverBackend.elements.models.elements.steps;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.CheckboxField;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class SummaryStepElement extends BaseInputElement<Boolean> {
    public SummaryStepElement() {
        super(ElementType.SummaryStep);
    }

    @Override
    public void performValidation(Boolean value) throws ValidationException {
        if (!Boolean.TRUE.equals(value)) {
            throw new ValidationException(this, "Bitte prüfen und bestätigen Sie die Zusammenfassung.");
        }
    }

    @Override
    public Boolean formatValue(Object value) {
        return CheckboxField._formatValue(value);
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable Boolean value) {
        return value == null || !value ? "Nicht bestätigt" : "Bestätigt";
    }
}
