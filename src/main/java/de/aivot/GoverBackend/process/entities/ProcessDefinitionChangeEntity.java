package de.aivot.GoverBackend.process.entities;

import de.aivot.GoverBackend.models.lib.DiffItem;
import de.aivot.GoverBackend.process.converters.DiffItemsConverter;
import de.aivot.GoverBackend.process.enums.ProcessChangeType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "process_definition_changes")
public class ProcessDefinitionChangeEntity {
    private static final String ID_SEQUENCE_NAME = "process_definition_changes_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Long id;

    @Nonnull
    @NotNull(message = "Der Zeitstempel darf nicht null sein.")
    private LocalDateTime timestamp;

    @Nonnull
    @NotNull(message = "Die Benutzer-ID darf nicht null sein.")
    @Length(min = 36, max = 36, message = "Die Benutzer-ID muss genau 36 Zeichen lang sein.")
    private String userId;

    @Nonnull
    @NotNull(message = "Die ID der Prozessdefinition darf nicht null sein.")
    private Integer processDefinitionId;

    @Nullable
    private Integer processDefinitionVersion;

    @Nullable
    private Integer processDefinitionNodeId;

    @Nullable
    private Integer processDefinitionEdgeId;

    @Nonnull
    @NotNull(message = "Der Änderungstyp darf nicht null sein.")
    private ProcessChangeType changeType;

    @Nonnull
    @NotNull(message = "Der Diff darf nicht null sein.")
    @Column(columnDefinition = "jsonb")
    @Convert(converter = DiffItemsConverter.class)
    private List<DiffItem> diff;

    @Nullable
    @Length(max = 512, message = "Der Kommentar darf maximal 512 Zeichen lang sein.")
    private String comment;

    // region Constructors

    // Empty constructor for JPA
    public ProcessDefinitionChangeEntity() {
    }

    // Full constructor
    public ProcessDefinitionChangeEntity(@Nonnull Long id,
                                         @Nonnull LocalDateTime timestamp,
                                         @Nonnull String userId,
                                         @Nonnull Integer processDefinitionId,
                                         @Nullable Integer processDefinitionVersion,
                                         @Nullable Integer processDefinitionNodeId,
                                         @Nullable Integer processDefinitionEdgeId,
                                         @Nonnull ProcessChangeType changeType,
                                         @Nonnull List<DiffItem> diff,
                                         @Nullable String comment) {
        this.id = id;
        this.timestamp = timestamp;
        this.userId = userId;
        this.processDefinitionId = processDefinitionId;
        this.processDefinitionVersion = processDefinitionVersion;
        this.processDefinitionNodeId = processDefinitionNodeId;
        this.processDefinitionEdgeId = processDefinitionEdgeId;
        this.changeType = changeType;
        this.diff = diff;
        this.comment = comment;
    }

    // endregion

    // region Equals and HasCode

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProcessDefinitionChangeEntity that = (ProcessDefinitionChangeEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(timestamp, that.timestamp) && Objects.equals(userId, that.userId) && Objects.equals(processDefinitionId, that.processDefinitionId) && Objects.equals(processDefinitionVersion, that.processDefinitionVersion) && Objects.equals(processDefinitionNodeId, that.processDefinitionNodeId) && Objects.equals(processDefinitionEdgeId, that.processDefinitionEdgeId) && changeType == that.changeType && Objects.equals(diff, that.diff) && Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, userId, processDefinitionId, processDefinitionVersion, processDefinitionNodeId, processDefinitionEdgeId, changeType, diff, comment);
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public Long getId() {
        return id;
    }

    public ProcessDefinitionChangeEntity setId(@Nonnull Long id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public ProcessDefinitionChangeEntity setTimestamp(@Nonnull LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public ProcessDefinitionChangeEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public Integer getProcessDefinitionId() {
        return processDefinitionId;
    }

    public ProcessDefinitionChangeEntity setProcessDefinitionId(@Nonnull Integer processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Nullable
    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public ProcessDefinitionChangeEntity setProcessDefinitionVersion(@Nullable Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
        return this;
    }

    @Nullable
    public Integer getProcessDefinitionNodeId() {
        return processDefinitionNodeId;
    }

    public ProcessDefinitionChangeEntity setProcessDefinitionNodeId(@Nullable Integer processDefinitionNodeId) {
        this.processDefinitionNodeId = processDefinitionNodeId;
        return this;
    }

    @Nullable
    public Integer getProcessDefinitionEdgeId() {
        return processDefinitionEdgeId;
    }

    public ProcessDefinitionChangeEntity setProcessDefinitionEdgeId(@Nullable Integer processDefinitionEdgeId) {
        this.processDefinitionEdgeId = processDefinitionEdgeId;
        return this;
    }

    @Nonnull
    public ProcessChangeType getChangeType() {
        return changeType;
    }

    public ProcessDefinitionChangeEntity setChangeType(@Nonnull ProcessChangeType changeType) {
        this.changeType = changeType;
        return this;
    }

    @Nonnull
    public List<DiffItem> getDiff() {
        return diff;
    }

    public ProcessDefinitionChangeEntity setDiff(@Nonnull List<DiffItem> diff) {
        this.diff = diff;
        return this;
    }

    @Nullable
    public String getComment() {
        return comment;
    }

    public ProcessDefinitionChangeEntity setComment(@Nullable String comment) {
        this.comment = comment;
        return this;
    }

    // endregion
}