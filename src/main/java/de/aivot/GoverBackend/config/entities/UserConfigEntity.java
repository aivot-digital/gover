package de.aivot.GoverBackend.config.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

import java.util.Objects;

@Entity
@Table(name = "user_configs")
@IdClass(UserConfigEntityId.class)
public class UserConfigEntity {
    @Id
    @Column(length = 36)
    private String userId;

    @Id
    @Column(length = 64)
    private String key;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String value;

    @NotNull
    @ColumnDefault("FALSE")
    private Boolean publicConfig;

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        UserConfigEntity that = (UserConfigEntity) object;
        return Objects.equals(userId, that.userId) && Objects.equals(key, that.key) && Objects.equals(value, that.value) && Objects.equals(publicConfig, that.publicConfig);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(userId);
        result = 31 * result + Objects.hashCode(key);
        result = 31 * result + Objects.hashCode(value);
        result = 31 * result + Objects.hashCode(publicConfig);
        return result;
    }

    public String getUserId() {
        return userId;
    }

    public UserConfigEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getKey() {
        return key;
    }

    public UserConfigEntity setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public UserConfigEntity setValue(String value) {
        this.value = value;
        return this;
    }

    public Boolean getPublicConfig() {
        return publicConfig;
    }

    public UserConfigEntity setPublicConfig(Boolean publicConfig) {
        this.publicConfig = publicConfig;
        return this;
    }
}
