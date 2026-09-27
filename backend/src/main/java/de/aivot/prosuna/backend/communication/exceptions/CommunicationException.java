package de.aivot.prosuna.backend.communication.exceptions;

public class CommunicationException extends Exception {
    public CommunicationException(String message) {
        super(message);
    }

    public CommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommunicationException(Throwable cause) {
        super(cause);
    }

    public CommunicationException(String format, Object... args) {
        super(format.formatted(args));
    }

    public CommunicationException(Throwable cause, String format, Object... args) {
        super(format.formatted(args), cause);
    }
}
