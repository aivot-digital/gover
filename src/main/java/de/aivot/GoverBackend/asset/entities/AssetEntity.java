package de.aivot.GoverBackend.asset.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Entity
@Table(name = "assets")
public class AssetEntity {
    @Id
    @Nonnull
    @NotNull(message = "Der Key darf nicht null sein.")
    private UUID key;

    @Nullable
    @Size(min = 36, max = 36, message = "Die Uploader ID muss genau 36 Zeichen lang sein.")
    @Column(length = 64)
    private String uploaderId;

    @Nonnull
    @NotNull(message = "Das Attribut isPrivate darf nicht null sein.")
    private Boolean isPrivate;

    @Nonnull
    @NotNull(message = "Die ID des Speicherindex-Items darf nicht null sein.")
    private Integer storageProviderId;

    @Nonnull
    @NotNull(message = "Der Pfad des Speicherindex-Items darf nicht null sein.")
    private String storagePathFromRoot;


    // region Getters & Setters

    @Nonnull
    public UUID getKey() {
        return key;
    }

    public AssetEntity setKey(@Nonnull UUID key) {
        this.key = key;
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
    public Boolean getPrivate() {
        return isPrivate;
    }

    public AssetEntity setPrivate(@Nonnull Boolean aPrivate) {
        isPrivate = aPrivate;
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

    // endregion
}
