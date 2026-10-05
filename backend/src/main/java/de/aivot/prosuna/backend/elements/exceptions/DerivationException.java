package de.aivot.prosuna.backend.elements.exceptions;

import de.aivot.prosuna.backend.elements.models.elements.BaseElement;

public class DerivationException extends Exception {
    public final BaseElement baseElement;

    public DerivationException(BaseElement baseElement, String message, Throwable cause) {
        super(message, cause);
        this.baseElement = baseElement;
    }

    public DerivationException(BaseElement baseElement, String message) {
        super(message);
        this.baseElement = baseElement;
    }
}
