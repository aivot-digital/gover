package de.aivot.GoverBackend.models.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.aivot.GoverBackend.converters.*;
import de.aivot.GoverBackend.models.elements.form.layout.GroupLayout;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "presets")
public class Preset {
    @Id
    @GeneratedValue
    private Long id;
    @Lob
    @Convert(converter = GroupLayoutConverter.class)
    @JsonSerialize(converter = JacksonGroupLayoutSerializer.class)
    @JsonDeserialize(converter = JacksonGroupLayoutDeserializer.class)
    private GroupLayout root;
    @CreationTimestamp
    private LocalDateTime created;
    @UpdateTimestamp
    private LocalDateTime updated;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GroupLayout getRoot() {
        return root;
    }

    public void setRoot(GroupLayout root) {
        this.root = root;
    }

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
}
