package de.aivot.GoverBackend.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "provider_links")
public class ProviderLink {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "provider_links_id_seq")
    @SequenceGenerator(name = "provider_links_id_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @Column(length = 128)
    @NotBlank(message = "Text cannot be blank")
    private String text;

    @NotNull
    @Column(length = 128)
    @NotNull(message = "Link cannot be blank")
    private String link;

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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
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
