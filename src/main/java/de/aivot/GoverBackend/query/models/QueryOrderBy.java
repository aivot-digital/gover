package de.aivot.GoverBackend.query.models;

import de.aivot.GoverBackend.query.exceptions.QueryValidationException;
import de.aivot.GoverBackend.query.utils.QueryPartMatcher;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class QueryOrderBy implements QueryPart {
    @Nullable
    private String columnName;
    @Nullable
    private Boolean ascending = true;

    // Empty constructor for deserialization
    public QueryOrderBy() {
    }

    // Full constructor
    public QueryOrderBy(@Nullable String columnName, @Nullable Boolean ascending) {
        this.columnName = columnName;
        this.ascending = ascending;
    }

    @Override
    public void validate() throws QueryValidationException {
        if (StringUtils.isNullOrEmpty(columnName)) {
            throw new QueryValidationException("Order by column name is required");
        }

        if (!QueryPartMatcher.checkColumnName(columnName)) {
            throw new QueryValidationException("Order by column name contains invalid characters");
        }
    }

    @Override
    public String build() throws QueryValidationException {
        validate();

        if (ascending == null || ascending) {
            return columnName + " ASC";
        } else {
            return columnName + " DESC";
        }
    }

    // region Generated

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        QueryOrderBy that = (QueryOrderBy) object;
        return Objects.equals(columnName, that.columnName) && Objects.equals(ascending, that.ascending);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(columnName);
        result = 31 * result + Objects.hashCode(ascending);
        return result;
    }

    @Nullable
    public String getColumnName() {
        return columnName;
    }

    public QueryOrderBy setColumnName(@Nullable String columnName) {
        this.columnName = columnName;
        return this;
    }

    @Nullable
    public Boolean getAscending() {
        return ascending;
    }

    public QueryOrderBy setAscending(@Nullable Boolean ascending) {
        this.ascending = ascending;
        return this;
    }

    // endregion
}
