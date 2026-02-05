package de.aivot.GoverBackend.storage.entities;

import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "storage_index_items")
@IdClass(StorageIndexItemEntityId.class)
public class StorageIndexItemEntity {
    @Id
    @Nonnull
    @NotNull(message = "Die ID des Speicheranbieters darf nicht null sein.")
    private Integer storageProviderId;

    @Nonnull
    @NotNull(message = "Der Typ des Speicheranbieters darf nicht null sein.")
    @Column(columnDefinition = "int2")
    private StorageProviderType storageProviderType;

    @Id
    @Nonnull
    @NotNull(message = "Der Pfad des Speicherobjekts darf nicht null sein.")
    @NotBlank(message = "Der Pfad des Speicherobjekts darf nicht leer sein sein.")
    @Size(max = 2048, message = "Der Pfad des Speicherobjekts darf maximal 2048 Zeichen lang sein.")
    private String pathFromRoot;

    @Nonnull
    @NotNull(message = "Gibt an, ob das Speicherobjekt ein Verzeichnis ist.")
    private Boolean isDirectory;

    @Nonnull
    @NotNull(message = "Der Name der Datei darf nicht null sein.")
    @NotBlank(message = "Der Name der Datei darf nicht leer sein sein.")
    @Size(max = 255, message = "Der Name der Datei darf maximal 255 Zeichen lang sein.")
    private String filename;

    @Nonnull
    @NotNull(message = "Der MIME-Typ der Datei darf nicht null sein.")
    @NotBlank(message = "Der MIME-Typ der Datei darf nicht leer sein sein.")
    @Size(max = 255, message = "Der MIME-Typ der Datei darf maximal 255 Zeichen lang sein.")
    private String mimeType;

    @Nonnull
    @NotNull(message = "Gibt an, ob das Speicherobjekt fehlt.")
    @ColumnDefault("FALSE")
    private Boolean isMissing;

    @Nonnull
    private LocalDateTime created;

    @Nonnull
    private LocalDateTime updated;

    public StorageIndexItemEntity() {
    }

    public StorageIndexItemEntity(@Nonnull Integer storageProviderId,
                                  @Nonnull StorageProviderType storageProviderType,
                                  @Nonnull String pathFromRoot,
                                  @Nonnull Boolean isDirectory,
                                  @Nonnull String filename,
                                  @Nonnull String mimeType,
                                  @Nonnull Boolean isMissing,
                                  @Nonnull LocalDateTime created,
                                  @Nonnull LocalDateTime updated) {
        this.storageProviderId = storageProviderId;
        this.storageProviderType = storageProviderType;
        this.pathFromRoot = pathFromRoot;
        this.isDirectory = isDirectory;
        this.filename = filename;
        this.mimeType = mimeType;
        this.isMissing = isMissing;
        this.created = created;
        this.updated = updated;
    }

    // region Signals

    @PrePersist
    protected void onCreate() {
        var now = LocalDateTime.now();
        created = now;
        updated = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updated = LocalDateTime.now();
    }

    @Nonnull
    public Integer getStorageProviderId() {
        return storageProviderId;
    }

    public StorageIndexItemEntity setStorageProviderId(@Nonnull Integer storageProviderId) {
        this.storageProviderId = storageProviderId;
        return this;
    }

    @Nonnull
    public StorageProviderType getStorageProviderType() {
        return storageProviderType;
    }

    public StorageIndexItemEntity setStorageProviderType(@Nonnull StorageProviderType storageProviderType) {
        this.storageProviderType = storageProviderType;
        return this;
    }

    @Nonnull
    public String getPathFromRoot() {
        return pathFromRoot;
    }

    public StorageIndexItemEntity setPathFromRoot(@Nonnull String pathFromRoot) {
        this.pathFromRoot = pathFromRoot;
        return this;
    }

    @Nonnull
    public Boolean getIsDirectory() {
        return isDirectory;
    }

    public StorageIndexItemEntity setIsDirectory(@Nonnull Boolean directory) {
        isDirectory = directory;
        return this;
    }

    @Nonnull
    public String getFilename() {
        return filename;
    }

    public StorageIndexItemEntity setFilename(@Nonnull String filename) {
        this.filename = filename;
        return this;
    }

    @Nonnull
    public String getMimeType() {
        return mimeType;
    }

    public StorageIndexItemEntity setMimeType(@Nonnull String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    @Nonnull
    public Boolean getIsMissing() {
        return isMissing;
    }

    public StorageIndexItemEntity setIsMissing(@Nonnull Boolean missing) {
        isMissing = missing;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public StorageIndexItemEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public StorageIndexItemEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }
}
