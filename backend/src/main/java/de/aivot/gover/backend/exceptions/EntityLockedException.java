package de.aivot.gover.backend.exceptions;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
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
