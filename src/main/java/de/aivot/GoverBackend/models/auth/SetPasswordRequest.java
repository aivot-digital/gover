package de.aivot.GoverBackend.models.auth;

import com.sun.istack.Nullable;

public class SetPasswordRequest {
    @Nullable
    private Integer userId;
    private String password;

    @Nullable
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
