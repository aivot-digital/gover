package de.aivot.GoverBackend.exceptions;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @deprecated Use {@link ResponseException} instead.
 */
@Deprecated
public class NotAcceptableException extends ResponseStatusException {
    public NotAcceptableException() {
        super(HttpStatus.NOT_ACCEPTABLE);
    }

    public NotAcceptableException(String reason) {
        super(HttpStatus.NOT_ACCEPTABLE, reason);
    }

    public NotAcceptableException(String template, Object ...args) {
        this(String.format(template, args));
    }
}
