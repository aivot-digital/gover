package de.aivot.GoverBackend.query.models;

import de.aivot.GoverBackend.query.enums.QueryWhereOperator;
import de.aivot.GoverBackend.query.exceptions.QueryValidationException;
import de.aivot.GoverBackend.query.utils.QueryPartMatcher;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class QueryWhere implements QueryPart {
    @Nullable
    private String columnName;
    @Nullable
    private QueryWhereOperator operator;
    @Nullable
    private String value;

    // Empty constructor for deserialization
    public QueryWhere() {

    }

    // Full constructor
    public QueryWhere(@Nullable String columnName, @Nullable QueryWhereOperator operator, @Nullable String value) {
        this.columnName = columnName;
        this.operator = operator;
        this.value = value;
    }


    @Override
    public void validate() throws QueryValidationException {
        if (StringUtils.isNullOrEmpty(columnName)) {
            throw new QueryValidationException("Where column name is required");
        }

        if (!QueryPartMatcher.checkColumnName(columnName)) {
            throw new QueryValidationException("Where column name contains invalid characters");
        }

        if (operator == null) {
            throw new QueryValidationException("Where operator is required");
        }

        if (StringUtils.isNullOrEmpty(value)) {
            throw new QueryValidationException("Where value is required");
        }

        if (!QueryPartMatcher.checkValue(value)) {
            throw new QueryValidationException("Where value contains invalid characters");
        }
    }

    @Override
    public String build() throws QueryValidationException {
        validate();

        var sqlOperator = operator.getSqlOperator();
        if (sqlOperator == null) {
            throw new QueryValidationException("Where operator is not supported");
        }

        return columnName + " " + sqlOperator + " '" + value + "'";
    }

    // region Generated

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        QueryWhere that = (QueryWhere) object;
        return Objects.equals(columnName, that.columnName) && operator == that.operator && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(columnName);
        result = 31 * result + Objects.hashCode(operator);
        result = 31 * result + Objects.hashCode(value);
        return result;
    }

    @Nullable
    public String getColumnName() {
        return columnName;
    }

    public QueryWhere setColumnName(@Nullable String columnName) {
        this.columnName = columnName;
        return this;
    }

    @Nullable
    public QueryWhereOperator getOperator() {
        return operator;
    }

    public QueryWhere setOperator(@Nullable QueryWhereOperator operator) {
        this.operator = operator;
        return this;
    }

    @Nullable
    public String getValue() {
        return value;
    }

    public QueryWhere setValue(@Nullable String value) {
        this.value = value;
        return this;
    }

    // endregion
}
