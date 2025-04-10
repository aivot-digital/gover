package de.aivot.GoverBackend.nocode.exceptions;

/**
 * Base exception for all exceptions that are thrown in the NoCode evaluation process.
 */
public class NoCodeException extends Exception {
    public NoCodeException(String message) {
        super(message);
    }
}
