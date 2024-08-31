package de.aivot.GoverBackend.exceptions;

import de.aivot.GoverBackend.models.elements.BaseElement;

public class RequiredValidationException extends ValidationException {
    public RequiredValidationException(BaseElement element) {
        super(element, "Field is required");
    }
}
