package de.aivot.prosuna.backend.codeLists.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.codeLists.converters.CodeListColumnMappingsConverter;
import de.aivot.prosuna.backend.codeLists.enums.CodeListSourceType;
import de.aivot.prosuna.backend.codeLists.enums.CodeListStatus;
import de.aivot.prosuna.backend.codeLists.models.CodeListColumnMapping;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Generated;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "code_lists")
public class CodeListEntity {
    @Id
    @Nonnull
    @Column(length = 255)
    @NotBlank(message = "Der Schlüssel der Codeliste darf nicht leer sein.")
    @Size(max = 255, message = "Der Schlüssel der Codeliste darf maximal 255 Zeichen lang sein.")
    private String key;
    @Nullable
    @Generated
    @Column(nullable = false, unique = true, insertable = false, updatable = false)
    private Integer id;
    @Nonnull
    private CodeListSourceType sourceType;
    @Nonnull
    private String sourceRef;
    @Nonnull
    private String name;
    @Nonnull
    private String description;
    // The source column identifiers (CSV header or XRepository column IDs) in the order of the item cells. A
    // synchronization matches source columns through them, so they are managed by the server.
    @Nonnull
    @Column(columnDefinition = "varchar(96)[]")
    private List<String> columns;
    @Nonnull
    @Convert(converter = CodeListColumnMappingsConverter.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<CodeListColumnMapping> columnMappings;
    // The key of the column whose values identify a row, e.g. to keep outdated rows during a synchronization.
    @Nullable
    private String keyColumnKey;
    @Nonnull
    private CodeListStatus status;
    @Nullable
    private String statusMessage;
    // The option of the requested synchronization, so a request that is published again keeps it.
    @Nonnull
    @JsonIgnore
    private Boolean syncKeepOutdated;
    // The run that claimed the requested synchronization and when it started. Only this run records its result.
    @Nullable
    @JsonIgnore
    private UUID syncRunId;
    @Nullable
    @JsonIgnore
    private Instant syncStarted;
    @Nullable
    private Instant lastSync;
    @Nonnull
    private Instant created;
    @Nonnull
    private Instant updated;

    @PrePersist
    public void prePersist() {
        if (sourceType == null) {
            sourceType = CodeListSourceType.Manual;
        }
        if (sourceRef == null) {
            sourceRef = "";
        }
        if (key != null) {
            key = key.trim();
        }
        if (columns == null) {
            columns = List.of();
        }
        if (columnMappings == null) {
            columnMappings = List.of();
        }
        if (status == null) {
            status = CodeListStatus.SyncPending;
        }
        if (syncKeepOutdated == null) {
            syncKeepOutdated = false;
        }
        var now = Instant.now();
        created = now;
        updated = now;
    }

    @PreUpdate
    public void preUpdate() {
        updated = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CodeListEntity that = (CodeListEntity) o;
        return Objects.equals(key, that.key) && Objects.equals(id, that.id) && sourceType == that.sourceType && Objects.equals(sourceRef, that.sourceRef) &&
                Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(columns, that.columns) &&
                Objects.equals(columnMappings, that.columnMappings) && Objects.equals(keyColumnKey, that.keyColumnKey) && status == that.status &&
                Objects.equals(statusMessage, that.statusMessage) && Objects.equals(syncKeepOutdated, that.syncKeepOutdated) && Objects.equals(syncRunId, that.syncRunId) &&
                Objects.equals(syncStarted, that.syncStarted) && Objects.equals(lastSync, that.lastSync) && Objects.equals(created, that.created) &&
                Objects.equals(updated, that.updated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, id, sourceType, sourceRef, name, description, columns, columnMappings, keyColumnKey, status, statusMessage, syncKeepOutdated, syncRunId, syncStarted, lastSync, created, updated);
    }

    @Nonnull
    public String getKey() {
        return key;
    }

    public CodeListEntity setKey(@Nonnull String key) {
        this.key = key;
        return this;
    }

    @Nullable
    public Integer getId() {
        return id;
    }

    public CodeListEntity setId(@Nullable Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public CodeListSourceType getSourceType() {
        return sourceType;
    }

    public CodeListEntity setSourceType(@Nonnull CodeListSourceType sourceType) {
        this.sourceType = sourceType;
        return this;
    }

    @Nonnull
    public String getSourceRef() {
        return sourceRef;
    }

    public CodeListEntity setSourceRef(@Nonnull String sourceRef) {
        this.sourceRef = sourceRef;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public CodeListEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    public CodeListEntity setDescription(@Nonnull String description) {
        this.description = description;
        return this;
    }

    @Nonnull
    public List<String> getColumns() {
        return columns;
    }

    public CodeListEntity setColumns(@Nonnull List<String> columns) {
        this.columns = columns;
        return this;
    }

    @Nonnull
    public List<CodeListColumnMapping> getColumnMappings() {
        return columnMappings;
    }

    public CodeListEntity setColumnMappings(@Nonnull List<CodeListColumnMapping> columnMappings) {
        this.columnMappings = columnMappings;
        return this;
    }

    @Nonnull
    public Optional<CodeListColumnMapping> findColumnMapping(@Nullable String columnKey) {
        return columnMappings
                .stream()
                .filter(mapping -> mapping.key().equals(columnKey))
                .findFirst();
    }

    @Nullable
    public String getKeyColumnKey() {
        return keyColumnKey;
    }

    public CodeListEntity setKeyColumnKey(@Nullable String keyColumnKey) {
        this.keyColumnKey = keyColumnKey;
        return this;
    }

    @Nonnull
    public CodeListStatus getStatus() {
        return status;
    }

    public CodeListEntity setStatus(@Nonnull CodeListStatus status) {
        this.status = status;
        return this;
    }

    @Nullable
    public String getStatusMessage() {
        return statusMessage;
    }

    public CodeListEntity setStatusMessage(@Nullable String statusMessage) {
        this.statusMessage = statusMessage;
        return this;
    }

    @Nonnull
    public Boolean getSyncKeepOutdated() {
        return syncKeepOutdated;
    }

    public CodeListEntity setSyncKeepOutdated(@Nonnull Boolean syncKeepOutdated) {
        this.syncKeepOutdated = syncKeepOutdated;
        return this;
    }

    @Nullable
    public UUID getSyncRunId() {
        return syncRunId;
    }

    public CodeListEntity setSyncRunId(@Nullable UUID syncRunId) {
        this.syncRunId = syncRunId;
        return this;
    }

    @Nullable
    public Instant getSyncStarted() {
        return syncStarted;
    }

    public CodeListEntity setSyncStarted(@Nullable Instant syncStarted) {
        this.syncStarted = syncStarted;
        return this;
    }

    @Nullable
    public Instant getLastSync() {
        return lastSync;
    }

    public CodeListEntity setLastSync(@Nullable Instant lastSync) {
        this.lastSync = lastSync;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public CodeListEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public CodeListEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }
}
