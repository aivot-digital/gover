package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

import java.util.Arrays;
import java.util.Optional;

public enum ConditionOperator implements Identifiable<Integer> {
    Equals(0),
    NotEquals(1),

    LessThan(2),
    LessThanOrEqual(3),
    GreaterThan(4),
    GreaterThanOrEqual(5),

    Includes(6),
    NotIncludes(7),
    StartsWith(8),
    NotStartsWith(9),
    EndsWith(10),
    NotEndsWith(11),

    MatchesPattern(12),
    NotMatchesPattern(13),
    IncludesPattern(14),
    NotIncludesPattern(15),
    EqualsIgnoreCase(16), // TODO: Implementieren
    NotEqualsIgnoreCase(17); // TODO: Implementieren

    private final Integer key;

    private ConditionOperator(Integer key) {
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

    public static Optional<ConditionOperator> findElement(Object id) {
        return Arrays
                .stream(ConditionOperator.values())
                .filter(e -> e.matches(id))
                .findFirst();
    }
}
