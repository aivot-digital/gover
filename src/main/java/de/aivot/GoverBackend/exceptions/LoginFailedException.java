package de.aivot.GoverBackend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LoginFailedException extends ResponseStatusException {
    public LoginFailedException(String reason) {
        super(HttpStatus.UNAUTHORIZED, reason);
    }
}
