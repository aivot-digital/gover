package de.aivot.GoverBackend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.enums.SystemConfigKey;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_configs")
public class SystemConfig {
    @Id
    private SystemConfigKey key;
    @NotBlank(message = "Value cannot be blank")
    private String value;
    @ColumnDefault("FALSE")
    private boolean isPublic;
    @CreationTimestamp
    private LocalDateTime created;
    @UpdateTimestamp
    private LocalDateTime updated;

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public SystemConfigKey getKey() {
        return key;
    }

    public void setKey(SystemConfigKey key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @JsonIgnore
    public boolean isPublic() {
        return isPublic;
    }

    @JsonIgnore
    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }
}
