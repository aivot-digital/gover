package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.models.Identifiable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum PermissionLevel implements Identifiable<Integer> {
    None(0),
    Read(1),
    Write(2);

    private final Integer key;

    PermissionLevel(Integer key) {
        this.key = key;
    }

    @Override
    @JsonValue
    public Integer getKey() {
        return key;
    }

    @Override
    public boolean matches(Object other) {
        return key.equals(other);
    }

    public boolean canRead() {
        return key >= Read.key;
    }

    public boolean unableToRead() {
        return !canRead();
    }

    public boolean canWrite() {
        return key >= Write.key;
    }

    public boolean unableToWrite() {
        return !canWrite();
    }

    public void testCanRead() {
        if (canRead()) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public void testCanWrite() {
        if (canWrite()) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
