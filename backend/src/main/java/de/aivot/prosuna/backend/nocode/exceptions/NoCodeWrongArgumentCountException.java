package de.aivot.prosuna.backend.nocode.exceptions;

/**
 * Exception that is thrown when a wrong argument count occurs during the NoCode evaluation process.
 */
public class NoCodeWrongArgumentCountException extends NoCodeException {
    public NoCodeWrongArgumentCountException(int expected, int actual) {
        super("Die Anzahl der Parameter passt nicht zur Funktion. Erwartet: " + expected + ". Vorhanden: " + actual + ".");
    }
}
