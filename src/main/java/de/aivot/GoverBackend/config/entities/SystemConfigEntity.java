package de.aivot.GoverBackend.config.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "system_configs")
public class SystemConfigEntity {
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

        SystemConfigEntity that = (SystemConfigEntity) object;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value) && Objects.equals(publicConfig, that.publicConfig);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(key);
        result = 31 * result + Objects.hashCode(value);
        result = 31 * result + Objects.hashCode(publicConfig);
        return result;
    }

    @JsonIgnore
    public Optional<String> getValueAsString() {
        return Optional.ofNullable(value);
    }


    @JsonIgnore
    public Optional<Boolean> getValueAsBoolean() {
        if (value == null) {
            return Optional.empty();
        }

        var bool = Boolean.parseBoolean(value);

        return Optional.of(bool);
    }

    @JsonIgnore
    public Optional<Integer> getValueAsInteger() {
        if (value == null) {
            return Optional.empty();
        }

        var integer = Integer.parseInt(value);

        return Optional.of(integer);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return value == null || StringUtils.isNullOrEmpty(value);
    }

    public String getKey() {
        return key;
    }

    public SystemConfigEntity setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public SystemConfigEntity setValue(String value) {
        this.value = value;
        return this;
    }

    public Boolean getPublicConfig() {
        return publicConfig;
    }

    public SystemConfigEntity setPublicConfig(Boolean publicConfig) {
        this.publicConfig = publicConfig;
        return this;
    }
}
