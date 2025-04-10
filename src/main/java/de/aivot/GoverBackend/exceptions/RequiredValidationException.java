package de.aivot.GoverBackend.exceptions;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.elements.models.BaseElement;

/**
 * @deprecated Use {@link ResponseException} instead.
 */
@Deprecated
public class RequiredValidationException extends ValidationException {
    public RequiredValidationException(BaseElement element) {
        super(element, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
    }
}
