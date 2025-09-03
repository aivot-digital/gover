package de.aivot.GoverBackend.models.lib;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class TestProtocol implements Serializable {
    @Nullable
    private String userId;
    @Nullable
    private String timestamp;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        TestProtocol that = (TestProtocol) o;
        return Objects.equals(userId, that.userId) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(userId);
        result = 31 * result + Objects.hashCode(timestamp);
        return result;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    public TestProtocol setUserId(@Nullable String userId) {
        this.userId = userId;
        return this;
    }

    @Nullable
    public String getTimestamp() {
        return timestamp;
    }

    public TestProtocol setTimestamp(@Nullable String timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}
