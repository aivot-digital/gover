package de.aivot.GoverBackend.exceptions;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @deprecated Use {@link ResponseException} instead.
 */
@Deprecated
public class BadRequestException extends ResponseStatusException {
    public BadRequestException() {
        super(HttpStatus.BAD_REQUEST);
    }

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, message, cause);
    }

    public BadRequestException(Throwable cause) {
        super(HttpStatus.BAD_REQUEST, cause.toString(), cause);
    }

    public BadRequestException(String message, Object ...args) {
        this(String.format(message, args));
    }
}
