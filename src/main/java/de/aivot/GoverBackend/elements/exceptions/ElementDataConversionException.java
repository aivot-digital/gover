package de.aivot.GoverBackend.elements.exceptions;

public class ElementDataConversionException extends Exception {
    public ElementDataConversionException(String message) {
        super(message);
    }

    public ElementDataConversionException(String format, Object ...args) {
        super(String.format(format, args));
    }
}
