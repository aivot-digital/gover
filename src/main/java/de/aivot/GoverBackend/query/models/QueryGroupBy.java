package de.aivot.GoverBackend.query.models;

import de.aivot.GoverBackend.query.exceptions.QueryValidationException;
import de.aivot.GoverBackend.query.utils.QueryPartMatcher;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class QueryGroupBy implements QueryPart {
    @Nullable
    private String columnName;

    // Empty constructor for deserialization
    public QueryGroupBy() {
    }

    // Full constructor
    public QueryGroupBy(@Nullable String columnName) {
        this.columnName = columnName;
    }

    @Override
    public void validate() throws QueryValidationException {
        if (StringUtils.isNullOrEmpty(columnName)) {
            throw new QueryValidationException("Group by column name is required");
        }

        if (!QueryPartMatcher.checkColumnName(columnName)) {
            throw new QueryValidationException("Group by column name contains invalid characters");
        }
    }

    @Override
    public String build() throws QueryValidationException {
        validate();
        return columnName;
    }

    // region Generated

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        QueryGroupBy that = (QueryGroupBy) object;
        return Objects.equals(columnName, that.columnName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(columnName);
    }

    @Nullable
    public String getColumnName() {
        return columnName;
    }

    public QueryGroupBy setColumnName(@Nullable String columnName) {
        this.columnName = columnName;
        return this;
    }

    // endregion
}
