package de.aivot.GoverBackend.models;

import javax.annotation.Nullable;

public class SetPasswordRequest {
    @Nullable
    private Long userId;
    private String password;

    @Nullable
    public Long getUserId() {
        return userId;
    }

    public void setUserId(@Nullable Long userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
