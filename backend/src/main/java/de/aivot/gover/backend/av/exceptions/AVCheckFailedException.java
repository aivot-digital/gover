package de.aivot.gover.backend.av.exceptions;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
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
