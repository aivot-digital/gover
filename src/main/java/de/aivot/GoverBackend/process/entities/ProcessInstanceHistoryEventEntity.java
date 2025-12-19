package de.aivot.GoverBackend.process.entities;

import de.aivot.GoverBackend.core.converters.JsonObjectConverter;
import de.aivot.GoverBackend.process.enums.ProcessHistoryEventType;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.logging.Logger;

@Entity
@Table(name = "process_instance_history_events")
public class ProcessInstanceHistoryEventEntity {
    private static final String ID_SEQUENCE_NAME = "process_instance_history_events_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Long id;

    @Nonnull
    @NotNull(message = "Der Typ des Ereignisses muss angegeben werden.")
    @Column(columnDefinition = "int2")
    private ProcessHistoryEventType type;

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

    @Nonnull
    @NotNull(message = "Die ID der Prozessinstanz muss angegeben werden.")
    private Long processInstanceId;

    @Nullable
    private Long processInstanceTaskId;

    // region Constructors

    // Default constructor for JPA
    public ProcessInstanceHistoryEventEntity() {
    }

    // Full constructor

    public ProcessInstanceHistoryEventEntity(@Nullable Long id,
                                             @Nonnull ProcessHistoryEventType type,
                                             @Nonnull String title,
                                             @Nonnull String message,
                                             @Nonnull Map<String, Object> details,
                                             @Nonnull LocalDateTime timestamp,
                                             @Nullable String triggeringUserId,
                                             @Nonnull Long processInstanceId,
                                             @Nullable Long processInstanceTaskId) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.details = details;
        this.timestamp = timestamp;
        this.triggeringUserId = triggeringUserId;
        this.processInstanceId = processInstanceId;
        this.processInstanceTaskId = processInstanceTaskId;
    }

    // endregion

    // region Factory Methods

    public static ProcessInstanceHistoryEventEntity from(
            @Nonnull Long processInstanceId,
            @Nullable Long processInstanceTaskId,
            @Nullable String triggeringUserId,
            @Nonnull Throwable err
    ) {
        return new ProcessInstanceHistoryEventEntity()
                .setType(ProcessHistoryEventType.Error)
                .setTitle("Fehler im Prozess")
                .setMessage(err.getLocalizedMessage())
                .setDetails(Map.of(
                        "errorType", err.getClass().getName(),
                        "errorMessage", err.getMessage()
                ))
                .setTimestamp(LocalDateTime.now())
                .setTriggeringUserId(triggeringUserId)
                .setProcessInstanceId(processInstanceId)
                .setProcessInstanceTaskId(processInstanceTaskId);
    }

    public static ProcessInstanceHistoryEventEntity from(
            @Nonnull Long processInstanceId,
            @Nullable Long processInstanceTaskId,
            @Nullable String triggeringUserId,
            @Nonnull ProcessNodeExecutionEvent event
    ) {
        return new ProcessInstanceHistoryEventEntity()
                .setType(event.type())
                .setTitle(event.title())
                .setMessage(event.message())
                .setDetails(event.details())
                .setTimestamp(LocalDateTime.now())
                .setTriggeringUserId(triggeringUserId)
                .setProcessInstanceId(processInstanceId)
                .setProcessInstanceTaskId(processInstanceTaskId);
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public Long getId() {
        return id;
    }

    public ProcessInstanceHistoryEventEntity setId(@Nonnull Long id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public ProcessHistoryEventType getType() {
        return type;
    }

    public ProcessInstanceHistoryEventEntity setType(@Nonnull ProcessHistoryEventType type) {
        this.type = type;
        return this;
    }

    @Nonnull
    public String getTitle() {
        return title;
    }

    public ProcessInstanceHistoryEventEntity setTitle(@Nonnull String title) {
        this.title = title;
        return this;
    }

    @Nonnull
    public String getMessage() {
        return message;
    }

    public ProcessInstanceHistoryEventEntity setMessage(@Nonnull String message) {
        this.message = message;
        return this;
    }

    @Nonnull
    public Map<String, Object> getDetails() {
        return details;
    }

    public ProcessInstanceHistoryEventEntity setDetails(@Nonnull Map<String, Object> details) {
        this.details = details;
        return this;
    }

    @Nonnull
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public ProcessInstanceHistoryEventEntity setTimestamp(@Nonnull LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Nullable
    public String getTriggeringUserId() {
        return triggeringUserId;
    }

    public ProcessInstanceHistoryEventEntity setTriggeringUserId(@Nullable String triggeringUserId) {
        this.triggeringUserId = triggeringUserId;
        return this;
    }

    @Nonnull
    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public ProcessInstanceHistoryEventEntity setProcessInstanceId(@Nonnull Long processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Nullable
    public Long getProcessInstanceTaskId() {
        return processInstanceTaskId;
    }

    public ProcessInstanceHistoryEventEntity setProcessInstanceTaskId(@Nullable Long processInstanceTaskId) {
        this.processInstanceTaskId = processInstanceTaskId;
        return this;
    }

    // endregion
}