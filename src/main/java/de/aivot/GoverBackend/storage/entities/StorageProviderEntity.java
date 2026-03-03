package de.aivot.GoverBackend.storage.entities;

import de.aivot.GoverBackend.core.converters.ElementDataConverter;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.storage.converters.StorageProviderMetadataAttributesConverter;
import de.aivot.GoverBackend.storage.enums.StorageProviderStatus;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import de.aivot.GoverBackend.storage.models.StorageProviderMetadataAttribute;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.List;
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
    @NotNull(message = "Die Read-Only-Eigenschaft des Speicheranbieters darf nicht null sein.")
    @ColumnDefault("FALSE")
    private Boolean readOnlyStorage;

    @Nonnull
    @NotNull(message = "Die vorproduktive Eigenschaft des Speicheranbieters darf nicht null sein.")
    @ColumnDefault("FALSE")
    private Boolean testProvider;

    @Nonnull
    @NotNull(message = "Die Konfiguration des Speicheranbieters darf nicht null sein.")
    @Column(columnDefinition = "jsonb")
    @Convert(converter = ElementDataConverter.class)
    private ElementData configuration;

    @Nonnull
    @NotNull(message = "Die maximale Dateigröße des Speicheranbieters darf nicht null sein.")
    @Min(value = 0, message = "Die maximale Dateigröße des Speicheranbieters muss größer oder gleich 0 sein.")
    @ColumnDefault("0")
    private Long maxFileSizeInBytes;

    @Nonnull
    @NotNull(message = "Die Verhinderung der Löschung des Speicheranbieters darf nicht null sein.")
    @ColumnDefault("FALSE")
    private Boolean systemProvider;

    @Nonnull
    @NotNull(message = "Die Liste der Metadatenattribute des Speicheranbieters darf nicht null sein.")
    @Column(columnDefinition = "jsonb")
    @Convert(converter = StorageProviderMetadataAttributesConverter.class)
    private List<StorageProviderMetadataAttribute> metadataAttributes;

    @Nullable
    private LocalDateTime lastSync;

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

    // region Constructors

    public StorageProviderEntity() {
    }

    public StorageProviderEntity(@Nonnull Integer id,
                                 @Nonnull String storageProviderDefinitionKey,
                                 @Nonnull Integer storageProviderDefinitionVersion,
                                 @Nonnull String name,
                                 @Nonnull String description,
                                 @Nonnull StorageProviderType type,
                                 @Nonnull StorageProviderStatus status,
                                 @Nullable String statusMessage,
                                 @Nonnull Boolean readOnlyStorage,
                                 @Nonnull Boolean testProvider,
                                 @Nonnull ElementData configuration,
                                 @Nonnull Long maxFileSizeInBytes,
                                 @Nonnull Boolean systemProvider,
                                 @Nonnull List<StorageProviderMetadataAttribute> metadataAttributes,
                                 @Nullable LocalDateTime lastSync,
                                 @Nonnull LocalDateTime created,
                                 @Nonnull LocalDateTime updated) {
        this.id = id;
        this.storageProviderDefinitionKey = storageProviderDefinitionKey;
        this.storageProviderDefinitionVersion = storageProviderDefinitionVersion;
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = status;
        this.statusMessage = statusMessage;
        this.readOnlyStorage = readOnlyStorage;
        this.configuration = configuration;
        this.maxFileSizeInBytes = maxFileSizeInBytes;
        this.systemProvider = systemProvider;
        this.testProvider = testProvider;
        this.metadataAttributes = metadataAttributes;
        this.lastSync = lastSync;
        this.created = created;
        this.updated = updated;
    }

    // endregion

    // region Equals & HashCode

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StorageProviderEntity that = (StorageProviderEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(storageProviderDefinitionKey, that.storageProviderDefinitionKey) && Objects.equals(storageProviderDefinitionVersion, that.storageProviderDefinitionVersion) && Objects.equals(name, that.name) && Objects.equals(description, that.description) && type == that.type && status == that.status && Objects.equals(statusMessage, that.statusMessage) && Objects.equals(readOnlyStorage, that.readOnlyStorage) && Objects.equals(testProvider, that.testProvider) && Objects.equals(configuration, that.configuration) && Objects.equals(maxFileSizeInBytes, that.maxFileSizeInBytes) && Objects.equals(systemProvider, that.systemProvider) && Objects.equals(metadataAttributes, that.metadataAttributes) && Objects.equals(lastSync, that.lastSync) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, storageProviderDefinitionKey, storageProviderDefinitionVersion, name, description, type, status, statusMessage, readOnlyStorage, testProvider, configuration, maxFileSizeInBytes, systemProvider, metadataAttributes, lastSync, created, updated);
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
    public Boolean getReadOnlyStorage() {
        return readOnlyStorage;
    }

    public StorageProviderEntity setReadOnlyStorage(@Nonnull Boolean readOnly) {
        this.readOnlyStorage = readOnly;
        return this;
    }

    @Nonnull
    public Boolean getTestProvider() {
        return testProvider;
    }

    public StorageProviderEntity setTestProvider(@Nonnull Boolean testProvider) {
        this.testProvider = testProvider;
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
    public Long getMaxFileSizeInBytes() {
        return maxFileSizeInBytes;
    }

    public StorageProviderEntity setMaxFileSizeInBytes(@Nonnull Long maxFileSizeInBytes) {
        this.maxFileSizeInBytes = maxFileSizeInBytes;
        return this;
    }

    @Nonnull
    public Boolean getSystemProvider() {
        return systemProvider;
    }

    public StorageProviderEntity setSystemProvider(@Nonnull Boolean preventDeletion) {
        this.systemProvider = preventDeletion;
        return this;
    }

    @Nonnull
    public List<StorageProviderMetadataAttribute> getMetadataAttributes() {
        return metadataAttributes;
    }

    public StorageProviderEntity setMetadataAttributes(@Nonnull List<StorageProviderMetadataAttribute> metadataAttributes) {
        this.metadataAttributes = metadataAttributes;
        return this;
    }

    @Nullable
    public LocalDateTime getLastSync() {
        return lastSync;
    }

    public StorageProviderEntity setLastSync(@Nullable LocalDateTime lastSync) {
        this.lastSync = lastSync;
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
