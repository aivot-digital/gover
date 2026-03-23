package de.aivot.GoverBackend.form.cache.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@RedisHash(value = "FormLock", timeToLive = 60 * 5) // Expire after 5 Minutes
public class FormLockCacheEntity implements Serializable {
    @Id
    private Integer formId;
    private String userId;

    public Integer getFormId() {
        return formId;
    }

    public FormLockCacheEntity setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public FormLockCacheEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}
