package de.aivot.GoverBackend.config.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "system_configs")
public class SystemConfigEntity {
    @Id
    @Nonnull
    @NotNull(message = "Der Key darf nicht null sein.")
    @NotBlank(message = "Der Key darf nicht leer sein.")
    @Size(min = 1, max = 64, message = "Der Key muss zwischen 1 und 64 Zeichen lang sein.")
    @Column(length = 64)
    private String key;

    @Nonnull
    @NotNull(message = "Der Wert darf nicht null sein.")
    @Column(columnDefinition = "TEXT")
    private String value;

    @Nonnull
    @NotNull(message = "Das publicConfig Feld darf nicht null sein.")
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

        Boolean bool = null;
        try {
            bool = Boolean.parseBoolean(value);
        } catch (Exception e) {
            return Optional.empty();
        }

        return Optional.of(bool);
    }

    @JsonIgnore
    public Optional<Integer> getValueAsInteger() {
        if (value == null) {
            return Optional.empty();
        }

        Integer integer = null;
        try {
            integer = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

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
