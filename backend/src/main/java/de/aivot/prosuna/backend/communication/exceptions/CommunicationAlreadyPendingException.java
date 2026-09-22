package de.aivot.prosuna.backend.communication.exceptions;

public class CommunicationAlreadyPendingException extends RuntimeException {
    public CommunicationAlreadyPendingException() {
        super("Für diese Aufgabe wurde bereits ein Versand gestartet.");
    }
}
