package de.aivot.GoverBackend.config.entities;

import java.io.Serializable;
import java.util.Objects;

public class UserConfigEntityId implements Serializable {
    private String userId;
    private String key;

    public UserConfigEntityId() {
    }

    public UserConfigEntityId(String userId, String key) {
        this.userId = userId;
        this.key = key;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        var that = (UserConfigEntityId) object;
        return Objects.equals(key, that.key) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(key);
        result = 31 * result + Objects.hashCode(userId);
        return result;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
