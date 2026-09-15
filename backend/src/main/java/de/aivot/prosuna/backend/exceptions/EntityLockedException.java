package de.aivot.prosuna.backend.exceptions;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.springframework.http.HttpStatus;

/**
 * @deprecated Use {@link ResponseException} instead.
 */
@Deprecated
public class EntityLockedException extends ResponseException {
    public EntityLockedException(String message) {
        super(HttpStatus.LOCKED, message);
    }
}
