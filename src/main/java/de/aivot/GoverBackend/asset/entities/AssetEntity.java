package de.aivot.GoverBackend.asset.entities;

import org.hibernate.validator.constraints.Length;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "assets")
public class AssetEntity {
    @Id
    @NotNull
    @Column(columnDefinition = "uuid")
    private String key;

    @NotNull
    @Column(length = 255)
    @Length(min = 3, max = 255)
    private String filename;

    @NotNull
    private LocalDateTime created;

    @NotNull
    @Column(length = 64)
    private String uploaderId;

    @Column(length = 255)
    private String contentType;

    @NotNull
    private Boolean isPrivate;

    // region Getters & Setters

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(String uploaderId) {
        this.uploaderId = uploaderId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Boolean getPrivate() {
        return isPrivate;
    }

    public void setPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    // endregion
}
