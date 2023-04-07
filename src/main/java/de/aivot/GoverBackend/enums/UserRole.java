package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

import java.util.Arrays;
import java.util.Optional;

public enum UserRole implements Identifiable<Integer> {
    Editor(0),
    Publisher(1),
    Admin(2);

    private final Integer key;

    private UserRole(Integer key) {
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

    public static Optional<UserRole> findElement(Object id) {
        return Arrays
                .stream(UserRole.values())
                .filter(e -> e.matches(id))
                .findFirst();
    }
}
