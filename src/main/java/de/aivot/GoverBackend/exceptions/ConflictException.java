package de.aivot.GoverBackend.exceptions;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @deprecated Use {@link ResponseException} instead.
 */
@Deprecated
public class ConflictException extends ResponseStatusException {
    public ConflictException() {
        super(HttpStatus.CONFLICT);
    }

    public ConflictException(String reason) {
        super(HttpStatus.CONFLICT, reason);
    }

    public ConflictException(String reason, Throwable cause) {
        super(HttpStatus.CONFLICT, reason, cause);
    }

    public ConflictException(Throwable cause) {
        super(HttpStatus.CONFLICT, cause.getMessage(), cause);
    }
}
