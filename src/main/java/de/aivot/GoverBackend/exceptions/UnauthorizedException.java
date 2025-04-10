package de.aivot.GoverBackend.exceptions;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @deprecated Use {@link ResponseException} instead.
 */
@Deprecated
public class UnauthorizedException extends ResponseStatusException {
    public UnauthorizedException() {
        super(HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
