package de.aivot.GoverBackend.query.enums;

import jakarta.annotation.Nullable;

public enum QueryWhereOperator {
    Equals,
    NotEquals,
    GreaterThan,
    LessThan,
    GreaterThanOrEquals,
    LessThanOrEquals,
    In,
    NotIn,
    Like,
    NotLike,
    IsNull,
    IsNotNull;

    @Nullable
    public String getSqlOperator() {
        return switch (this) {
            case Equals -> "=";
            case NotEquals -> "<>";
            case GreaterThan -> ">";
            case LessThan -> "<";
            case GreaterThanOrEquals -> ">=";
            case LessThanOrEquals -> "<=";
            case In -> "IN";
            case NotIn -> "NOT IN";
            case Like -> "LIKE";
            case NotLike -> "NOT LIKE";
            case IsNull -> "IS NULL";
            case IsNotNull -> "IS NOT NULL";
            default -> null;
        };
    }
}
