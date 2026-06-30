package de.aivot.gover.backend.config.entities;

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

    // region HashCode & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserConfigEntity that = (UserConfigEntity) o;
        return Objects.equals(userId, that.userId) && Objects.equals(key, that.key) && Objects.equals(value, that.value) && Objects.equals(publicConfig, that.publicConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, key, value, publicConfig);
    }

    // endregion

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
