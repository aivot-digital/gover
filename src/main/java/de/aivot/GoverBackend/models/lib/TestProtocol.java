package de.aivot.GoverBackend.models.lib;

import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;
import java.util.Objects;

public class TestProtocol {
    private String userId;
    private String timestamp;

    public TestProtocol(Map<String, Object> data) {
        userId = MapUtils.getString(data, "userId");
        timestamp = MapUtils.getString(data, "timestamp");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestProtocol that = (TestProtocol) o;

        if (!Objects.equals(userId, that.userId)) return false;
        return Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
