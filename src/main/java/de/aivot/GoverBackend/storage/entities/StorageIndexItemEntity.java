package de.aivot.GoverBackend.storage.entities;

import de.aivot.GoverBackend.core.converters.JsonObjectConverter;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

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
    private Boolean directory;

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
    private Boolean missing;

    @Nonnull
    @NotNull(message = "Die Metadaten des Speicherobjekts dürfen nicht null sein.")
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    private Map<String, Object> metadata;

    @Nonnull
    private LocalDateTime created;

    @Nonnull
    private LocalDateTime updated;

    // region Constructors

    public StorageIndexItemEntity() {
    }

    public StorageIndexItemEntity(@Nonnull Integer storageProviderId,
                                  @Nonnull StorageProviderType storageProviderType,
                                  @Nonnull String pathFromRoot,
                                  @Nonnull Boolean directory,
                                  @Nonnull String filename,
                                  @Nonnull String mimeType,
                                  @Nonnull Boolean missing,
                                  @Nonnull Map<String, Object> metadata,
                                  @Nonnull LocalDateTime created,
                                  @Nonnull LocalDateTime updated) {
        this.storageProviderId = storageProviderId;
        this.storageProviderType = storageProviderType;
        this.pathFromRoot = pathFromRoot;
        this.directory = directory;
        this.filename = filename;
        this.mimeType = mimeType;
        this.missing = missing;
        this.metadata = metadata;
        this.created = created;
        this.updated = updated;
    }

    // endregion

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

    // endregion

    // region Equals and HashCode

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StorageIndexItemEntity that = (StorageIndexItemEntity) o;
        return Objects.equals(storageProviderId, that.storageProviderId) && storageProviderType == that.storageProviderType && Objects.equals(pathFromRoot, that.pathFromRoot) && Objects.equals(directory, that.directory) && Objects.equals(filename, that.filename) && Objects.equals(mimeType, that.mimeType) && Objects.equals(missing, that.missing) && Objects.equals(metadata, that.metadata) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storageProviderId, storageProviderType, pathFromRoot, directory, filename, mimeType, missing, metadata, created, updated);
    }

    // endregion

    // region Getters and Setters

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
    public Boolean getDirectory() {
        return directory;
    }

    public StorageIndexItemEntity setDirectory(@Nonnull Boolean directory) {
        this.directory = directory;
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
    public Boolean getMissing() {
        return missing;
    }

    public StorageIndexItemEntity setMissing(@Nonnull Boolean missing) {
        this.missing = missing;
        return this;
    }

    @Nonnull
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public StorageIndexItemEntity setMetadata(@Nonnull Map<String, Object> metadata) {
        this.metadata = metadata;
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

    // endregion
}
