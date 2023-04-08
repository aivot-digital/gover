package de.aivot.GoverBackend.exceptions;

import de.aivot.GoverBackend.models.elements.BaseElement;

public class ValidationException extends Exception {
    private final String message;
    private final BaseElement element;

    public ValidationException(BaseElement element, String message) {
        this.element = element;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public BaseElement getElement() {
        return element;
    }
}
