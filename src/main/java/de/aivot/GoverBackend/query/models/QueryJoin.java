package de.aivot.GoverBackend.query.models;

import de.aivot.GoverBackend.query.exceptions.QueryValidationException;
import de.aivot.GoverBackend.query.utils.QueryPartMatcher;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class QueryJoin implements QueryPart {
    @Nullable
    private String tableName;
    @Nullable
    private List<QueryJoinColumPair> onColumns;

    // Empty constructor for deserialization
    public QueryJoin() {
    }

    // Full constructor


    public QueryJoin(@Nullable String tableName,
                     @Nullable List<QueryJoinColumPair> onColumns) {
        this.tableName = tableName;
        this.onColumns = onColumns;
    }

    @Override
    public void validate() throws QueryValidationException {
        if (StringUtils.isNullOrEmpty(tableName)) {
            throw new QueryValidationException("Join table name is required");
        }

        if (!QueryPartMatcher.checkTableName(tableName)) {
            throw new QueryValidationException("Join table name contains invalid characters");
        }

        if (onColumns == null || onColumns.isEmpty()) {
            throw new QueryValidationException("At least one join column is required");
        }

        for (var col : onColumns) {
            col.validate();
        }
    }

    @Override
    public String build() throws QueryValidationException {
        validate();

        var sb = new StringBuilder();
        sb
                .append(tableName)
                .append(" ON ");

        if (onColumns != null) {
            var colBuffer = new LinkedList<String>();
            for (var col : onColumns) {
                colBuffer.add(col.build());
            }

            sb.append(String.join(" AND ", colBuffer));
        }

        return sb.toString();
    }

    // region Generated

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        QueryJoin queryJoin = (QueryJoin) object;
        return Objects.equals(tableName, queryJoin.tableName) && Objects.equals(onColumns, queryJoin.onColumns);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(tableName);
        result = 31 * result + Objects.hashCode(onColumns);
        return result;
    }

    @Nullable
    public String getTableName() {
        return tableName;
    }

    public QueryJoin setTableName(@Nullable String tableName) {
        this.tableName = tableName;
        return this;
    }

    @Nullable
    public List<QueryJoinColumPair> getOnColumns() {
        return onColumns;
    }

    public QueryJoin setOnColumns(@Nullable List<QueryJoinColumPair> onColumns) {
        this.onColumns = onColumns;
        return this;
    }

    // endregion
}
