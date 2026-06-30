package de.aivot.gover.backend.exceptions;

import de.aivot.gover.backend.elements.models.elements.BaseElement;

import java.util.HashMap;

public class ValidationException extends Exception {
    private final String message;
    private final Object errorDetails;
    private final BaseElement element;

    public ValidationException(BaseElement element, String message) {
        this.element = element;
        this.message = message;
        this.errorDetails = new HashMap<>();
    }

    public ValidationException(BaseElement element, String message, Object errorDetails) {
        this.element = element;
        this.message = message;
        this.errorDetails = errorDetails;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public BaseElement getElement() {
        return element;
    }

    public Object getErrorDetails() {
        return errorDetails;
    }
}
