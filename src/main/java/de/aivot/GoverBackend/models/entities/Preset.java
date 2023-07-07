package de.aivot.GoverBackend.models.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.aivot.GoverBackend.converters.*;
import de.aivot.GoverBackend.models.elements.form.layout.GroupLayout;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "presets")
public class Preset {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "presets_id_seq")
    @SequenceGenerator(name = "presets_id_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @Convert(converter = GroupLayoutConverter.class)
    @JsonSerialize(converter = JacksonGroupLayoutSerializer.class)
    @JsonDeserialize(converter = JacksonGroupLayoutDeserializer.class)
    @Column(columnDefinition = "jsonb")
    private GroupLayout root;

    @NotNull
    private LocalDateTime created;

    @NotNull
    private LocalDateTime updated;

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = LocalDateTime.now();
    }

    // region Getters & Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    // endregion
}
