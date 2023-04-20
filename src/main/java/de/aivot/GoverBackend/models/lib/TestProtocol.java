package de.aivot.GoverBackend.models.lib;

import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;

public class TestProtocol {
    private Integer userId;
    private String timestamp;

    public TestProtocol(Map<String, Object> data) {
        userId = MapUtils.getInteger(data, "userId");
        timestamp = MapUtils.getString(data, "timestamp");
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
