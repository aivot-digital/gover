package de.aivot.prosuna.backend.av.exceptions;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.springframework.http.HttpStatus;

public class AVCheckFailedException extends ResponseException {
    public AVCheckFailedException(String details) {
        super(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Die Datei konnte nicht auf Schadsoftware geprüft werden.",
                details
        );
    }

    public AVCheckFailedException(String details, Throwable cause) {
        super(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Die Datei konnte nicht auf Schadsoftware geprüft werden.",
                details,
                cause
        );
    }
}
