package de.aivot.GoverBackend.query.models;

import de.aivot.GoverBackend.query.enums.QueryAggregator;
import de.aivot.GoverBackend.query.exceptions.QueryValidationException;
import de.aivot.GoverBackend.query.utils.QueryPartMatcher;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class QueryColumn implements QueryPart {
    @Nullable
    private String columnName;
    @Nullable
    private QueryAggregator aggregator;
    @Nullable
    private String asColumnName;

    // Default constructor for deserialization
    public QueryColumn() {
    }

    // Full constructor
    public QueryColumn(@Nullable String columnName,
                       @Nullable QueryAggregator aggregator,
                       @Nullable String asColumnName) {
        this.columnName = columnName;
        this.aggregator = aggregator;
        this.asColumnName = asColumnName;
    }

    @Override
    public void validate() throws QueryValidationException {
        if (StringUtils.isNullOrEmpty(columnName)) {
            throw new QueryValidationException("Column name is required");
        }

        if (!QueryPartMatcher.checkColumnName(columnName)) {
            throw new QueryValidationException("Column name contains invalid characters");
        }

        if (aggregator != null && StringUtils.isNullOrEmpty(asColumnName)) {
            throw new QueryValidationException("Alias column name is required when using an aggregator");
        }

        if (asColumnName != null) {
            if (StringUtils.isNullOrEmpty(asColumnName)) {
                throw new QueryValidationException("Alias column name is required or null");
            }

            if (!QueryPartMatcher.checkColumnName(asColumnName)) {
                throw new QueryValidationException("Alias column name contains invalid characters");
            }
        }
    }

    @Override
    public String build() throws QueryValidationException {
        validate();

        StringBuilder sb = new StringBuilder();

        if (aggregator != null) {
            sb.append(aggregator.name()).append("(").append(columnName).append(")");
        } else {
            sb.append(columnName);
        }

        if (asColumnName != null) {
            sb.append(" AS ").append(asColumnName);
        }

        return sb.toString();
    }

    // region Generated

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        QueryColumn that = (QueryColumn) object;
        return Objects.equals(columnName, that.columnName) && aggregator == that.aggregator && Objects.equals(asColumnName, that.asColumnName);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(columnName);
        result = 31 * result + Objects.hashCode(aggregator);
        result = 31 * result + Objects.hashCode(asColumnName);
        return result;
    }

    @Nullable
    public String getColumnName() {
        return columnName;
    }

    public QueryColumn setColumnName(@Nullable String columnName) {
        this.columnName = columnName;
        return this;
    }

    @Nullable
    public QueryAggregator getAggregator() {
        return aggregator;
    }

    public QueryColumn setAggregator(@Nullable QueryAggregator aggregator) {
        this.aggregator = aggregator;
        return this;
    }

    @Nullable
    public String getAsColumnName() {
        return asColumnName;
    }

    public QueryColumn setAsColumnName(@Nullable String asColumnName) {
        this.asColumnName = asColumnName;
        return this;
    }

// endregion
}
