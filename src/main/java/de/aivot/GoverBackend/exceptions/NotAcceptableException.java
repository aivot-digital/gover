package de.aivot.GoverBackend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
