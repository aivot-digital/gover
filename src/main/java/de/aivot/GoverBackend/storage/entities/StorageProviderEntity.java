package de.aivot.GoverBackend.storage.entities;

import de.aivot.GoverBackend.core.converters.ElementDataConverter;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.storage.enums.StorageProviderStatus;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "storage_providers")
public class StorageProviderEntity {
    private static final String ID_SEQUENCE_NAME = "storage_providers_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    @NotNull(message = "Die ID des Speicheranbieters darf nicht null sein.")
    private Integer id;

    @Nonnull
    @NotNull(message = "Der Definition-Schlüssel des Speicheranbieters darf nicht null sein.")
    @NotBlank(message = "Der Definition-Schlüssel des Speicheranbieters darf nicht leer sein.")
    @Size(max = 255, message = "Der Definition-Schlüssel des Speicheranbieters darf maximal 255 Zeichen lang sein.")
    private String storageProviderDefinitionKey;

    @Nonnull
    @NotNull(message = "Die Definitionsversion des Speicheranbieters darf nicht null sein.")
    private Integer storageProviderDefinitionVersion;

    @Nonnull
    @NotNull(message = "Der Name des Speicheranbieters darf nicht null sein.")
    @NotBlank(message = "Der Name des Speicheranbieters darf nicht leer sein.")
    @Size(max = 255, message = "Der Name des Speicheranbieters darf maximal 255 Zeichen lang sein.")
    private String name;

    @Nonnull
    @NotNull(message = "Die Beschreibung des Speicheranbieters darf nicht null sein.")
    @Size(max = 1024, message = "Die Beschreibung des Speicheranbieters darf maximal 1024 Zeichen lang sein.")
    private String description;

    @Nonnull
    @NotNull(message = "Der Typ des Speicheranbieters darf nicht null sein.")
    @Column(columnDefinition = "int2")
    private StorageProviderType type;

    @Nonnull
    @NotNull(message = "Der Status des Speicheranbieters darf nicht null sein.")
    @Column(columnDefinition = "int2")
    private StorageProviderStatus status;

    @Nullable
    @Size(max = 1024, message = "Die Statusnachricht des Speicheranbieters darf maximal 1024 Zeichen lang sein.")
    private String statusMessage;

    @Nonnull
    @NotNull(message = "Die Konfiguration des Speicheranbieters darf nicht null sein.")
    @Column(columnDefinition = "jsonb")
    @Convert(converter = ElementDataConverter.class)
    private ElementData configuration;

    @Nonnull
    private LocalDateTime created;

    @Nonnull
    private LocalDateTime updated;

    // region Signals

     @PrePersist
    protected void onCreate() {
        created = LocalDateTime.now();
        updated = created;
    }

    @PreUpdate
    protected void onUpdate() {
        updated = LocalDateTime.now();
    }

    // endregion

    // region Equals & HashCode

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StorageProviderEntity that = (StorageProviderEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(storageProviderDefinitionKey, that.storageProviderDefinitionKey) && Objects.equals(storageProviderDefinitionVersion, that.storageProviderDefinitionVersion) && Objects.equals(name, that.name) && Objects.equals(description, that.description) && type == that.type && status == that.status && Objects.equals(statusMessage, that.statusMessage) && Objects.equals(configuration, that.configuration) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, storageProviderDefinitionKey, storageProviderDefinitionVersion, name, description, type, status, statusMessage, configuration, created, updated);
    }


    // endregion

    // region Getters & Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public StorageProviderEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getStorageProviderDefinitionKey() {
        return storageProviderDefinitionKey;
    }

    public StorageProviderEntity setStorageProviderDefinitionKey(@Nonnull String storageProviderDefinitionKey) {
        this.storageProviderDefinitionKey = storageProviderDefinitionKey;
        return this;
    }

    @Nonnull
    public Integer getStorageProviderDefinitionVersion() {
        return storageProviderDefinitionVersion;
    }

    public StorageProviderEntity setStorageProviderDefinitionVersion(@Nonnull Integer storageProviderDefinitionVersion) {
        this.storageProviderDefinitionVersion = storageProviderDefinitionVersion;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public StorageProviderEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    public StorageProviderEntity setDescription(@Nonnull String description) {
        this.description = description;
        return this;
    }

    @Nonnull
    public StorageProviderType getType() {
        return type;
    }

    public StorageProviderEntity setType(@Nonnull StorageProviderType type) {
        this.type = type;
        return this;
    }

    @Nonnull
    public StorageProviderStatus getStatus() {
        return status;
    }

    public StorageProviderEntity setStatus(@Nonnull StorageProviderStatus status) {
        this.status = status;
        return this;
    }

    @Nullable
    public String getStatusMessage() {
        return statusMessage;
    }

    public StorageProviderEntity setStatusMessage(@Nullable String statusMessage) {
        this.statusMessage = statusMessage;
        return this;
    }

    @Nonnull
    public ElementData getConfiguration() {
        return configuration;
    }

    public StorageProviderEntity setConfiguration(@Nonnull ElementData configuration) {
        this.configuration = configuration;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public StorageProviderEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public StorageProviderEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}
