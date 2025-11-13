package de.aivot.GoverBackend.query.models;

import com.beust.jcommander.Strings;
import de.aivot.GoverBackend.query.exceptions.QueryValidationException;
import de.aivot.GoverBackend.query.utils.QueryPartMatcher;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class QuerySelect implements QueryPart {
    @Nullable
    private List<QueryColumn> columns;
    @Nullable
    private String tableName;
    @Nullable
    private List<QueryJoin> join;
    @Nullable
    private List<QueryWhere> where;
    @Nullable
    private List<QueryGroupBy> groupBy;
    @Nullable
    private List<QueryOrderBy> orderBy;
    @Nullable
    private Integer limit;

    // Empty constructor for deserialization
    public QuerySelect() {

    }

    // Full constructor


    public QuerySelect(@Nullable List<QueryColumn> columns,
                       @Nullable String tableName,
                       @Nullable List<QueryJoin> join,
                       @Nullable List<QueryWhere> where,
                       @Nullable List<QueryGroupBy> groupBy,
                       @Nullable List<QueryOrderBy> orderBy,
                       @Nullable Integer limit) {
        this.columns = columns;
        this.tableName = tableName;
        this.join = join;
        this.where = where;
        this.groupBy = groupBy;
        this.orderBy = orderBy;
        this.limit = limit;
    }

    @Override
    public void validate() throws QueryValidationException {
        if (columns == null || columns.isEmpty()) {
            throw new QueryValidationException("At least one column is required");
        }

        for (var col : columns) {
            col.validate();
        }

        if (StringUtils.isNullOrEmpty(tableName)) {
            throw new QueryValidationException("Table name is required");
        }

        if (!QueryPartMatcher.checkTableName(tableName)) {
            throw new QueryValidationException("Table name contains invalid characters");
        }

        if (join != null) {
            for (var joinCol : join) {
                joinCol.validate();
            }
        }

        if (where != null) {
            for (var where : where) {
                where.validate();
            }
        }

        if (groupBy != null) {
            for (var group : groupBy) {
                group.validate();
            }
        }

        if (orderBy != null) {
            for (var order : orderBy) {
                order.validate();
            }
        }

        if (limit != null && limit <= 0) {
            throw new QueryValidationException("Limit must be greater than zero");
        }
    }

    @Override
    public String build() throws QueryValidationException {
        validate();

        var sb = new StringBuilder();

        // Start with select
        sb.append("SELECT \n");

        // Append all columns
        if (columns != null && !columns.isEmpty()) {
            var columnBuffer = new LinkedList<String>();
            for (var column : columns) {
                columnBuffer.add(column.build());
            }

            sb
                    .append(" ")
                    .append(Strings.join(",\n ", columnBuffer))
                    .append("\n");
        }

        // From table
        sb
                .append("FROM ")
                .append(tableName)
                .append("\n");

        // If join is present, build and append it
        if (join != null) {
            sb.append(" JOIN \n");

            for (var join : join) {
                sb
                        .append("   ")
                        .append(join.build())
                        .append("\n");
            }
        }

        // Where clause
        if (where != null && !where.isEmpty()) {
            sb.append("WHERE \n");

            var whereBuffer = new LinkedList<String>();
            for (var where : where) {
                whereBuffer.add(where.build());
            }

            sb
                    .append("    ")
                    .append(Strings.join(" AND ", whereBuffer))
                    .append("\n");
        }


        if (groupBy != null && !groupBy.isEmpty()) {
            sb.append("GROUP BY \n");

            var groupBuffer = new LinkedList<String>();
            for (var group : groupBy) {
                groupBuffer.add(group.build());
            }

            sb
                    .append("   ")
                    .append(Strings.join(", ", groupBuffer))
                    .append("\n");
        }
        if (orderBy != null && !orderBy.isEmpty()) {
            sb.append("ORDER BY ");
            for (int i = 0; i < orderBy.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(orderBy.get(i).build());
            }
        }
        if (limit != null) {
            sb.append("LIMIT ").append(limit);
        }

        return sb.toString();
    }

    // region Generated

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        QuerySelect that = (QuerySelect) object;
        return Objects.equals(columns, that.columns) && Objects.equals(tableName, that.tableName) && Objects.equals(join, that.join) && Objects.equals(where, that.where) && Objects.equals(groupBy, that.groupBy) && Objects.equals(orderBy, that.orderBy) && Objects.equals(limit, that.limit);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(columns);
        result = 31 * result + Objects.hashCode(tableName);
        result = 31 * result + Objects.hashCode(join);
        result = 31 * result + Objects.hashCode(where);
        result = 31 * result + Objects.hashCode(groupBy);
        result = 31 * result + Objects.hashCode(orderBy);
        result = 31 * result + Objects.hashCode(limit);
        return result;
    }

    @Nullable
    public List<QueryColumn> getColumns() {
        return columns;
    }

    public QuerySelect setColumns(@Nullable List<QueryColumn> columns) {
        this.columns = columns;
        return this;
    }

    @Nullable
    public String getTableName() {
        return tableName;
    }

    public QuerySelect setTableName(@Nullable String tableName) {
        this.tableName = tableName;
        return this;
    }

    @Nullable
    public List<QueryJoin> getJoin() {
        return join;
    }

    public QuerySelect setJoin(@Nullable List<QueryJoin> join) {
        this.join = join;
        return this;
    }

    @Nullable
    public List<QueryWhere> getWhere() {
        return where;
    }

    public QuerySelect setWhere(@Nullable List<QueryWhere> where) {
        this.where = where;
        return this;
    }

    @Nullable
    public List<QueryGroupBy> getGroupBy() {
        return groupBy;
    }

    public QuerySelect setGroupBy(@Nullable List<QueryGroupBy> groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    @Nullable
    public List<QueryOrderBy> getOrderBy() {
        return orderBy;
    }

    public QuerySelect setOrderBy(@Nullable List<QueryOrderBy> orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    @Nullable
    public Integer getLimit() {
        return limit;
    }

    public QuerySelect setLimit(@Nullable Integer limit) {
        this.limit = limit;
        return this;
    }

// endregion
}
