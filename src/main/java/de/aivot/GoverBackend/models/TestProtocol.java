package de.aivot.GoverBackend.models;

import java.util.Map;

public class TestProtocol {
    private Integer userId;
    private String timestamp;

    public TestProtocol(Map<String, Object> data) {
        if (data != null) {
            userId = (Integer) data.get("userId");
            timestamp = (String) data.get("timestamp");
        }
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
