package de.aivot.GoverBackend.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;


public class UserFriendlyResponseStatusException extends ResponseStatusException {
    public UserFriendlyResponseStatusException(HttpStatusCode status, String reason) {
        super(status, reason);
    }
}
