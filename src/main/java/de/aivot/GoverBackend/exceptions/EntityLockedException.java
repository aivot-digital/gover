package de.aivot.GoverBackend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class EntityLockedException extends ResponseStatusException {
    public EntityLockedException() {
        super(HttpStatus.LOCKED);
    }
}
