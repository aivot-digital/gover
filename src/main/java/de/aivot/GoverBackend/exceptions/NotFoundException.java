package de.aivot.GoverBackend.exceptions;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @deprecated Use {@link ResponseException} instead.
 */
@Deprecated
public class NotFoundException extends ResponseStatusException {
    public NotFoundException() {
        super(HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, message, cause);
    }

    public NotFoundException(Throwable cause) {
        super(HttpStatus.NOT_FOUND, cause.toString(), cause);
    }
}
