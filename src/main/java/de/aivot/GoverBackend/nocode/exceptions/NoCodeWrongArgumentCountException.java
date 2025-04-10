package de.aivot.GoverBackend.nocode.exceptions;

/**
 * Exception that is thrown when a wrong argument count occurs during the NoCode evaluation process.
 */
public class NoCodeWrongArgumentCountException extends NoCodeException {
    public NoCodeWrongArgumentCountException(int expected, int actual) {
        super("Wrong argument count. Expected " + expected + " but got " + actual);
    }
}
