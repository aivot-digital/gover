package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.models.Identifiable;

import java.util.Arrays;
import java.util.Optional;

public enum ConditionSetOperator implements Identifiable<Integer> {
    All(0),
    Any(1);

    private final Integer key;

    private ConditionSetOperator(Integer key) {
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

    public static Optional<ConditionSetOperator> findElement(Object id) {
        return Arrays
                .stream(ConditionSetOperator.values())
                .filter(e -> e.matches(id))
                .findFirst();
    }
}
