package de.aivot.GoverBackend.asset.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
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

    @Nullable
    @Size(min = 36, max = 36, message = "Die Uploader ID muss genau 36 Zeichen lang sein.")
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

    @Nonnull
    @NotNull(message = "Die ID des Speicherindex-Items darf nicht null sein.")
    private Integer storageProviderId;

    @Nonnull
    @NotNull(message = "Der Pfad des Speicherindex-Items darf nicht null sein.")
    private String storagePathFromRoot;

    @Transient
    @JsonIgnore
    private byte[] fileBytes;

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

    @Nullable
    public String getUploaderId() {
        return uploaderId;
    }

    public AssetEntity setUploaderId(@Nullable String uploaderId) {
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

    @Nonnull
    public Integer getStorageProviderId() {
        return storageProviderId;
    }

    public AssetEntity setStorageProviderId(@Nonnull Integer storageProviderId) {
        this.storageProviderId = storageProviderId;
        return this;
    }

    @Nonnull
    public String getStoragePathFromRoot() {
        return storagePathFromRoot;
    }

    public AssetEntity setStoragePathFromRoot(@Nonnull String storagePathFromRoot) {
        this.storagePathFromRoot = storagePathFromRoot;
        return this;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public AssetEntity setFileBytes(byte[] fileBytes) {
        this.fileBytes = fileBytes;
        return this;
    }

    // endregion
}
