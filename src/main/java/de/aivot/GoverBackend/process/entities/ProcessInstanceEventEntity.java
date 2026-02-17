package de.aivot.GoverBackend.process.entities;

import de.aivot.GoverBackend.core.converters.JsonObjectConverter;
import de.aivot.GoverBackend.process.enums.ProcessHistoryEventType;
import de.aivot.GoverBackend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.GoverBackend.process.models.XProcessNodeExecutionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "process_instance_events")
public class ProcessInstanceEventEntity {
    private static final String ID_SEQUENCE_NAME = "process_instance_events_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Long id;

    @Nonnull
    @NotNull(message = "Die ID der Prozessinstanz muss angegeben werden.")
    private Long processInstanceId;

    @Nullable
    private Long processInstanceTaskId;

    @Nonnull
    @NotNull(message = "Das Level des Ereignisses muss angegeben werden.")
    @Column(columnDefinition = "int2")
    private ProcessNodeExecutionLogLevel level;

    @Nonnull
    @NotNull(message = "Die Flag ob es sich um ein technisches Ereignis handelt muss angegeben werden.")
    @ColumnDefault("FALSE")
    private Boolean isTechnical = false;

    @Nonnull
    @NotNull(message = "Die Flag ob es sich um ein Audit-Ereignis handelt muss angegeben werden.")
    @ColumnDefault("FALSE")
    private Boolean isAudit = false;

    @Nonnull
    @NotNull(message = "Der Titel des Ereignisses muss angegeben werden.")
    @NotBlank(message = "Der Titel des Ereignisses darf nicht leer sein.")
    @Size(min = 3, max = 96, message = "Der Titel des Ereignisses muss zwischen 3 und 96 Zeichen lang sein.")
    private String title;

    @Nonnull
    @NotNull(message = "Die Nachricht des Ereignisses muss angegeben werden.")
    @NotBlank(message = "Die Nachricht des Ereignisses darf nicht leer sein.")
    @Size(min = 3, max = 2048, message = "Die Nachricht des Ereignisses muss zwischen 3 und 2048 Zeichen lang sein.")
    private String message;

    @Nonnull
    @NotNull(message = "Die Details des Ereignisses müssen angegeben werden.")
    @Convert(converter = JsonObjectConverter.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Nonnull
    @NotNull(message = "Der Zeitstempel des Ereignisses muss angegeben werden.")
    private LocalDateTime timestamp;

    @Nullable
    @Size(min = 36, max = 36, message = "Die ID des auslösenden Benutzers muss genau 36 Zeichen lang sein.")
    private String triggeringUserId;


    // region Constructors

    // Default constructor for JPA
    public ProcessInstanceEventEntity() {
    }

    // Full constructor

    public ProcessInstanceEventEntity(@Nonnull Long id,
                                      @Nonnull Long processInstanceId,
                                      @Nullable Long processInstanceTaskId,
                                      @Nonnull ProcessNodeExecutionLogLevel level,
                                      @Nonnull Boolean isTechnical,
                                      @Nonnull Boolean isAudit,
                                      @Nonnull String title,
                                      @Nonnull String message,
                                      @Nonnull Map<String, Object> details,
                                      @Nonnull LocalDateTime timestamp,
                                      @Nullable String triggeringUserId) {
        this.id = id;
        this.processInstanceId = processInstanceId;
        this.processInstanceTaskId = processInstanceTaskId;
        this.level = level;
        this.isTechnical = isTechnical;
        this.isAudit = isAudit;
        this.title = title;
        this.message = message;
        this.details = details;
        this.timestamp = timestamp;
        this.triggeringUserId = triggeringUserId;
    }


    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProcessInstanceEventEntity that = (ProcessInstanceEventEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(processInstanceId, that.processInstanceId) && Objects.equals(processInstanceTaskId, that.processInstanceTaskId) && level == that.level && Objects.equals(isTechnical, that.isTechnical) && Objects.equals(isAudit, that.isAudit) && Objects.equals(title, that.title) && Objects.equals(message, that.message) && Objects.equals(details, that.details) && Objects.equals(timestamp, that.timestamp) && Objects.equals(triggeringUserId, that.triggeringUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, processInstanceId, processInstanceTaskId, level, isTechnical, isAudit, title, message, details, timestamp, triggeringUserId);
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public Long getId() {
        return id;
    }

    public ProcessInstanceEventEntity setId(@Nonnull Long id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public ProcessInstanceEventEntity setProcessInstanceId(@Nonnull Long processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Nullable
    public Long getProcessInstanceTaskId() {
        return processInstanceTaskId;
    }

    public ProcessInstanceEventEntity setProcessInstanceTaskId(@Nullable Long processInstanceTaskId) {
        this.processInstanceTaskId = processInstanceTaskId;
        return this;
    }

    @Nonnull
    public ProcessNodeExecutionLogLevel getLevel() {
        return level;
    }

    public ProcessInstanceEventEntity setLevel(@Nonnull ProcessNodeExecutionLogLevel level) {
        this.level = level;
        return this;
    }

    @Nonnull
    public Boolean getTechnical() {
        return isTechnical;
    }

    public ProcessInstanceEventEntity setTechnical(@Nonnull Boolean technical) {
        isTechnical = technical;
        return this;
    }

    @Nonnull
    public Boolean getAudit() {
        return isAudit;
    }

    public ProcessInstanceEventEntity setAudit(@Nonnull Boolean audit) {
        isAudit = audit;
        return this;
    }

    @Nonnull
    public String getTitle() {
        return title;
    }

    public ProcessInstanceEventEntity setTitle(@Nonnull String title) {
        this.title = title;
        return this;
    }

    @Nonnull
    public String getMessage() {
        return message;
    }

    public ProcessInstanceEventEntity setMessage(@Nonnull String message) {
        this.message = message;
        return this;
    }

    @Nonnull
    public Map<String, Object> getDetails() {
        return details;
    }

    public ProcessInstanceEventEntity setDetails(@Nonnull Map<String, Object> details) {
        this.details = details;
        return this;
    }

    @Nonnull
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public ProcessInstanceEventEntity setTimestamp(@Nonnull LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Nullable
    public String getTriggeringUserId() {
        return triggeringUserId;
    }

    public ProcessInstanceEventEntity setTriggeringUserId(@Nullable String triggeringUserId) {
        this.triggeringUserId = triggeringUserId;
        return this;
    }

    // endregion
}