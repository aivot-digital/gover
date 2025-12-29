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
@Table(name = "process_changes")
public class ProcessChangeEntity {
    private static final String ID_SEQUENCE_NAME = "process_changes_id_seq";

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
    private Integer processId;

    @Nullable
    private Integer processVersion;

    @Nullable
    private Integer processNodeId;

    @Nullable
    private Integer processEdgeId;

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
    public ProcessChangeEntity() {
    }

    // Full constructor
    public ProcessChangeEntity(@Nonnull Long id,
                               @Nonnull LocalDateTime timestamp,
                               @Nonnull String userId,
                               @Nonnull Integer processId,
                               @Nullable Integer processVersion,
                               @Nullable Integer processNodeId,
                               @Nullable Integer processEdgeId,
                               @Nonnull ProcessChangeType changeType,
                               @Nonnull List<DiffItem> diff,
                               @Nullable String comment) {
        this.id = id;
        this.timestamp = timestamp;
        this.userId = userId;
        this.processId = processId;
        this.processVersion = processVersion;
        this.processNodeId = processNodeId;
        this.processEdgeId = processEdgeId;
        this.changeType = changeType;
        this.diff = diff;
        this.comment = comment;
    }

    // endregion

    // region Equals and HasCode

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProcessChangeEntity that = (ProcessChangeEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(timestamp, that.timestamp) && Objects.equals(userId, that.userId) && Objects.equals(processId, that.processId) && Objects.equals(processVersion, that.processVersion) && Objects.equals(processNodeId, that.processNodeId) && Objects.equals(processEdgeId, that.processEdgeId) && changeType == that.changeType && Objects.equals(diff, that.diff) && Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, userId, processId, processVersion, processNodeId, processEdgeId, changeType, diff, comment);
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public Long getId() {
        return id;
    }

    public ProcessChangeEntity setId(@Nonnull Long id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public ProcessChangeEntity setTimestamp(@Nonnull LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public ProcessChangeEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public Integer getProcessId() {
        return processId;
    }

    public ProcessChangeEntity setProcessId(@Nonnull Integer processDefinitionId) {
        this.processId = processDefinitionId;
        return this;
    }

    @Nullable
    public Integer getProcessVersion() {
        return processVersion;
    }

    public ProcessChangeEntity setProcessVersion(@Nullable Integer processDefinitionVersion) {
        this.processVersion = processDefinitionVersion;
        return this;
    }

    @Nullable
    public Integer getProcessNodeId() {
        return processNodeId;
    }

    public ProcessChangeEntity setProcessNodeId(@Nullable Integer processDefinitionNodeId) {
        this.processNodeId = processDefinitionNodeId;
        return this;
    }

    @Nullable
    public Integer getProcessEdgeId() {
        return processEdgeId;
    }

    public ProcessChangeEntity setProcessEdgeId(@Nullable Integer processDefinitionEdgeId) {
        this.processEdgeId = processDefinitionEdgeId;
        return this;
    }

    @Nonnull
    public ProcessChangeType getChangeType() {
        return changeType;
    }

    public ProcessChangeEntity setChangeType(@Nonnull ProcessChangeType changeType) {
        this.changeType = changeType;
        return this;
    }

    @Nonnull
    public List<DiffItem> getDiff() {
        return diff;
    }

    public ProcessChangeEntity setDiff(@Nonnull List<DiffItem> diff) {
        this.diff = diff;
        return this;
    }

    @Nullable
    public String getComment() {
        return comment;
    }

    public ProcessChangeEntity setComment(@Nullable String comment) {
        this.comment = comment;
        return this;
    }

    // endregion
}