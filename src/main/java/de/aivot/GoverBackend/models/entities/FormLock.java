package de.aivot.GoverBackend.models.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@RedisHash(value = "FormLock", timeToLive = 60 * 5) // Expire after 5 Minutes
public class FormLock implements Serializable {
    @Id
    private Integer formId;
    private String userId;

    public Integer getFormId() {
        return formId;
    }

    public void setFormId(Integer formId) {
        this.formId = formId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
