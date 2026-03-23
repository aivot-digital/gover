package de.aivot.GoverBackend.exceptions;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;

public class RequiredValidationException extends ValidationException {
    public RequiredValidationException(BaseElement element) {
        super(element, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
    }
}
