package de.aivot.prosuna.backend.exceptions;

import de.aivot.prosuna.backend.elements.models.elements.BaseElement;

public class RequiredValidationException extends ValidationException {
    public RequiredValidationException(BaseElement element) {
        super(element, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
    }
}
