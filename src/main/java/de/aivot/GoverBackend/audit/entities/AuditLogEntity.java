package de.aivot.GoverBackend.audit.entities;

import de.aivot.GoverBackend.core.converters.JsonObjectConverter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    private static final String ID_SEQUENCE_NAME = "audit_logs_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Long id;

    @Nonnull
    @NotNull(message = "Der Zeitstempel darf nicht null sein.")
    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Nonnull
    @NotNull(message = "Der Akteur-Typ darf nicht null sein.")
    @Length(max = 32, message = "Der Akteur-Typ darf maximal 32 Zeichen lang sein.")
    private String actorType;

    @Nullable
    @Length(max = 255, message = "Die Akteur-ID darf maximal 255 Zeichen lang sein.")
    private String actorId;

    @Nonnull
    @NotNull(message = "Der Trigger-Typ darf nicht null sein.")
    @Length(max = 64, message = "Der Trigger-Typ darf maximal 64 Zeichen lang sein.")
    private String triggerType;

    @Nullable
    @Length(max = 255, message = "Die Trigger-Referenz darf maximal 255 Zeichen lang sein.")
    private String triggerRef;

    @Nullable
    @Length(max = 64, message = "Der Trigger-Referenz-Typ darf maximal 64 Zeichen lang sein.")
    private String triggerRefType;

    @Nonnull
    @NotNull(message = "Das Modul darf nicht null sein.")
    @Length(max = 128, message = "Das Modul darf maximal 128 Zeichen lang sein.")
    private String module;

    @Nullable
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    private Map<String, Object> diff;

    @Nonnull
    @NotNull(message = "Metadata dürfen nicht null sein.")
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    private Map<String, Object> metadata;

    @Nullable
    @Length(max = 64, message = "Die IP-Adresse darf maximal 64 Zeichen lang sein.")
    @Column(name = "ipaddress")
    private String ipAddress;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AuditLogEntity that = (AuditLogEntity) o;
        return Objects.equals(id, that.id)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(actorType, that.actorType)
                && Objects.equals(actorId, that.actorId)
                && Objects.equals(triggerType, that.triggerType)
                && Objects.equals(triggerRef, that.triggerRef)
                && Objects.equals(triggerRefType, that.triggerRefType)
                && Objects.equals(module, that.module)
                && Objects.equals(diff, that.diff)
                && Objects.equals(metadata, that.metadata)
                && Objects.equals(ipAddress, that.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, actorType, actorId, triggerType, triggerRef, triggerRefType, module, diff, metadata, ipAddress);
    }

    @Nonnull
    public Long getId() {
        return id;
    }

    public AuditLogEntity setId(@Nullable Long id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public AuditLogEntity setTimestamp(@Nonnull LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Nonnull
    public String getActorType() {
        return actorType;
    }

    public AuditLogEntity setActorType(@Nonnull String actorType) {
        this.actorType = actorType;
        return this;
    }

    @Nullable
    public String getActorId() {
        return actorId;
    }

    public AuditLogEntity setActorId(@Nullable String actorId) {
        this.actorId = actorId;
        return this;
    }

    @Nonnull
    public String getTriggerType() {
        return triggerType;
    }

    public AuditLogEntity setTriggerType(@Nonnull String triggerType) {
        this.triggerType = triggerType;
        return this;
    }

    @Nullable
    public String getTriggerRef() {
        return triggerRef;
    }

    public AuditLogEntity setTriggerRef(@Nullable String triggerRef) {
        this.triggerRef = triggerRef;
        return this;
    }

    @Nullable
    public String getTriggerRefType() {
        return triggerRefType;
    }

    public AuditLogEntity setTriggerRefType(@Nullable String triggerRefType) {
        this.triggerRefType = triggerRefType;
        return this;
    }

    @Nonnull
    public String getModule() {
        return module;
    }

    public AuditLogEntity setModule(@Nonnull String module) {
        this.module = module;
        return this;
    }

    @Nullable
    public Map<String, Object> getDiff() {
        return diff;
    }

    public AuditLogEntity setDiff(@Nullable Map<String, Object> diff) {
        this.diff = diff;
        return this;
    }

    @Nonnull
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public AuditLogEntity setMetadata(@Nonnull Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    @Nullable
    public String getIpAddress() {
        return ipAddress;
    }

    public AuditLogEntity setIpAddress(@Nullable String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }
}
