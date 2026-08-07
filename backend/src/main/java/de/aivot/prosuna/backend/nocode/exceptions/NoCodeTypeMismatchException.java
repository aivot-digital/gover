package de.aivot.prosuna.backend.nocode.exceptions;

import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;

/**
 * Exception that is thrown when a type mismatch occurs during the NoCode evaluation process.
 */
public class NoCodeTypeMismatchException extends NoCodeException {
    public NoCodeTypeMismatchException(NoCodeDataType expected, NoCodeDataType actual) {
        super("Type mismatch. Expected " + expected + " but got " + actual);
    }
}
