package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

import java.util.Arrays;
import java.util.Optional;

public enum ConditionOperator implements Identifiable<Integer> {
    Equals(0, false),
    NotEquals(1, false),

    LessThan(2, false),
    LessThanOrEqual(3, false),
    GreaterThan(4, false),
    GreaterThanOrEqual(5, false),

    Includes(6, false),
    NotIncludes(7, false),
    StartsWith(8, false),
    NotStartsWith(9, false),
    EndsWith(10, false),
    NotEndsWith(11, false),

    MatchesPattern(12, false),
    NotMatchesPattern(13, false),
    IncludesPattern(14, false),
    NotIncludesPattern(15, false),
    EqualsIgnoreCase(16, false),
    NotEqualsIgnoreCase(17, false),

    Empty(18, true),
    NotEmpty(19, true),

    YearsInPast(20, false),
    MonthsInPast(21, false),
    DaysInPast(22, false),
    YearsInFuture(23, false),
    MonthsInFuture(24, false),
    DaysInFuture(25, false),

    ReplicatingListLengthEquals(26, false),
    ReplicatingListLengthNotEquals(27, false),
    ReplicatingListLengthLessThan(28, false),
    ReplicatingListLengthLessThanOrEqual(29, false),
    ReplicatingListLengthGreaterThan(30, false),
    ReplicatingListLengthGreaterThanOrEqual(31, false),
    ;

    private final Integer key;
    private final Boolean isUnary;

    private ConditionOperator(Integer key, Boolean isUnary) {
        this.key = key;
        this.isUnary = isUnary;
    }

    @Override
    @JsonValue
    public Integer getKey() {
        return key;
    }

    public Boolean getUnary() {
        return isUnary;
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
