package de.aivot.GoverBackend.query.models;

import de.aivot.GoverBackend.query.exceptions.QueryValidationException;
import de.aivot.GoverBackend.query.utils.QueryPartMatcher;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public class QueryJoinColumPair implements QueryPart {
    @Nullable
    private String leftColumnName;
    @Nullable
    private String rightColumnName;

    // Empty constructor for deserialization
    public QueryJoinColumPair() {
    }

    // Full constructor
    public QueryJoinColumPair(@Nullable String leftColumnName,
                              @Nullable String rightColumnName) {
        this.leftColumnName = leftColumnName;
        this.rightColumnName = rightColumnName;
    }

    @Override
    public void validate() throws QueryValidationException {
        if (StringUtils.isNullOrEmpty(leftColumnName)) {
            throw new QueryValidationException("Left join column name is required");
        }

        if (!QueryPartMatcher.checkColumnName(leftColumnName)) {
            throw new QueryValidationException("Left join column name contains invalid characters");
        }

        if (StringUtils.isNullOrEmpty(rightColumnName)) {
            throw new QueryValidationException("Right join column name is required");
        }

        if (!QueryPartMatcher.checkColumnName(rightColumnName)) {
            throw new QueryValidationException("Right join column name contains invalid characters");
        }
    }

    @Override
    public String build() throws QueryValidationException {
        validate();

        return leftColumnName + " = " + rightColumnName;
    }

    // region Generated

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        QueryJoinColumPair that = (QueryJoinColumPair) object;
        return Objects.equals(leftColumnName, that.leftColumnName) && Objects.equals(rightColumnName, that.rightColumnName);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(leftColumnName);
        result = 31 * result + Objects.hashCode(rightColumnName);
        return result;
    }

    @Nullable
    public String getLeftColumnName() {
        return leftColumnName;
    }

    public QueryJoinColumPair setLeftColumnName(@Nullable String leftColumnName) {
        this.leftColumnName = leftColumnName;
        return this;
    }

    @Nullable
    public String getRightColumnName() {
        return rightColumnName;
    }

    public QueryJoinColumPair setRightColumnName(@Nullable String rightColumnName) {
        this.rightColumnName = rightColumnName;
        return this;
    }

    // endregion
}
