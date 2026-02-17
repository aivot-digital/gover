package de.aivot.GoverBackend.asset.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assets")
public class AssetEntity {
    @Id
    @Nonnull
    @NotNull(message = "Der Key darf nicht null sein.")
    private UUID key;

    @Nonnull
    @Column(length = 255)
    @NotNull(message = "Der Dateiname darf nicht null sein.")
    @NotBlank(message = "Der Dateiname darf nicht leer sein.")
    @Size(min = 3, max = 255, message = "Der Dateiname muss zwischen 3 und 255 Zeichen lang sein.")
    private String filename;

    @Nonnull
    @NotNull(message = "Die Uploader ID darf nicht null sein.")
    @Size(min = 36, max = 364, message = "Die Uploader ID muss zwischen 36 und 64 Zeichen lang sein.")
    @Column(length = 64)
    private String uploaderId;

    @Nonnull
    @Column(length = 255)
    @NotNull(message = "Der Content Type darf nicht null sein.")
    @NotBlank(message = "Der Content Type darf nicht leer sein.")
    @Size(min = 3, max = 255, message = "Der Content Type muss zwischen 3 und 255 Zeichen lang sein.")
    private String contentType;

    @Nonnull
    @NotNull(message = "Das Attribut isPrivate darf nicht null sein.")
    private Boolean isPrivate;

    @Nonnull
    @NotNull(message = "Das Erstellungsdatum darf nicht null sein.")
    private LocalDateTime created;

    // region Getters & Setters

    @Nonnull
    public UUID getKey() {
        return key;
    }

    public AssetEntity setKey(@Nonnull UUID key) {
        this.key = key;
        return this;
    }

    @Nonnull
    public String getFilename() {
        return filename;
    }

    public AssetEntity setFilename(@Nonnull String filename) {
        this.filename = filename;
        return this;
    }

    @Nonnull
    public String getUploaderId() {
        return uploaderId;
    }

    public AssetEntity setUploaderId(@Nonnull String uploaderId) {
        this.uploaderId = uploaderId;
        return this;
    }

    @Nonnull
    public String getContentType() {
        return contentType;
    }

    public AssetEntity setContentType(@Nonnull String contentType) {
        this.contentType = contentType;
        return this;
    }

    @Nonnull
    public Boolean getPrivate() {
        return isPrivate;
    }

    public AssetEntity setPrivate(@Nonnull Boolean aPrivate) {
        isPrivate = aPrivate;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public AssetEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    // endregion
}
