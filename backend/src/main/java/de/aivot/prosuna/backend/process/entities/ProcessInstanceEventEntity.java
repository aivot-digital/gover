package de.aivot.prosuna.backend.process.entities;

import de.aivot.prosuna.backend.core.converters.JsonObjectConverter;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
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
    @NotNull(message = "Die ID des Vorgangs muss angegeben werden.")
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
    @NotNull(message = "Die Angabe, ob das Ereignis für den Verlauf relevant ist, muss gesetzt sein.")
    @ColumnDefault("FALSE")
    private Boolean isHistoryRelevant = false;

    @Nonnull
    @NotNull(message = "Der Titel des Ereignisses muss angegeben werden.")
    @NotBlank(message = "Der Titel des Ereignisses darf nicht leer sein.")
    @Size(min = 3, max = 96, message = "Der Titel des Ereignisses muss zwischen 3 und 96 Zeichen lang sein.")
    private String title;

    @Nonnull
    @NotNull(message = "Die Nachricht des Ereignisses muss angegeben werden.")
    @NotBlank(message = "Die Nachricht des Ereignisses darf nicht leer sein.")
    @Size(min = 3, max = 4096, message = "Die Nachricht des Ereignisses muss zwischen 3 und 4096 Zeichen lang sein.")
    private String message;

    @Nonnull
    @NotNull(message = "Die Details des Ereignisses müssen angegeben werden.")
    @Convert(converter = JsonObjectConverter.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Nonnull
    @NotNull(message = "Der Zeitstempel des Ereignisses muss angegeben werden.")
    @Column(columnDefinition = "timestamp with time zone")
    private Instant timestamp;

    @Nullable
    @Size(min = 36, max = 36, message = "Die ID des auslösenden Benutzers muss genau 36 Zeichen lang sein.")
    private String triggeringUserId;

    @Nullable
    @Size(min = 36, max = 36, message = "Die ID der betroffenen Person muss genau 36 Zeichen lang sein.")
    private String concernedUserId;

    @Nullable
    @Size(max = 255, message = "Die ID der betroffenen Identität darf maximal 255 Zeichen lang sein.")
    private String concernedIdentityId;

    @Nullable
    @Size(max = 255, message = "Der Titel der betroffenen Identität darf maximal 255 Zeichen lang sein.")
    private String concernedIdentityTitle;


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
                                      @Nonnull Boolean isHistoryRelevant,
                                      @Nonnull String title,
                                      @Nonnull String message,
                                      @Nonnull Map<String, Object> details,
                                      @Nonnull Instant timestamp,
                                      @Nullable String triggeringUserId,
                                      @Nullable String concernedUserId,
                                      @Nullable String concernedIdentityId,
                                      @Nullable String concernedIdentityTitle) {
        this.id = id;
        this.processInstanceId = processInstanceId;
        this.processInstanceTaskId = processInstanceTaskId;
        this.level = level;
        this.isTechnical = isTechnical;
        this.isAudit = isAudit;
        this.isHistoryRelevant = isHistoryRelevant;
        this.title = title;
        this.message = message;
        this.details = details;
        this.timestamp = timestamp;
        this.triggeringUserId = triggeringUserId;
        this.concernedUserId = concernedUserId;
        this.concernedIdentityId = concernedIdentityId;
        this.concernedIdentityTitle = concernedIdentityTitle;
    }


    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProcessInstanceEventEntity that = (ProcessInstanceEventEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(processInstanceId, that.processInstanceId) && Objects.equals(processInstanceTaskId, that.processInstanceTaskId) && level == that.level && Objects.equals(isTechnical, that.isTechnical) && Objects.equals(isAudit, that.isAudit) && Objects.equals(isHistoryRelevant, that.isHistoryRelevant) && Objects.equals(title, that.title) && Objects.equals(message, that.message) && Objects.equals(details, that.details) && Objects.equals(timestamp, that.timestamp) && Objects.equals(triggeringUserId, that.triggeringUserId) && Objects.equals(concernedUserId, that.concernedUserId) && Objects.equals(concernedIdentityId, that.concernedIdentityId) && Objects.equals(concernedIdentityTitle, that.concernedIdentityTitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, processInstanceId, processInstanceTaskId, level, isTechnical, isAudit, isHistoryRelevant, title, message, details, timestamp, triggeringUserId, concernedUserId, concernedIdentityId, concernedIdentityTitle);
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
    public Boolean getHistoryRelevant() {
        return isHistoryRelevant;
    }

    public ProcessInstanceEventEntity setHistoryRelevant(@Nonnull Boolean historyRelevant) {
        isHistoryRelevant = historyRelevant;
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
    public Instant getTimestamp() {
        return timestamp;
    }

    public ProcessInstanceEventEntity setTimestamp(@Nonnull Instant timestamp) {
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

    @Nullable
    public String getConcernedUserId() {
        return concernedUserId;
    }

    public ProcessInstanceEventEntity setConcernedUserId(@Nullable String concernedUserId) {
        this.concernedUserId = concernedUserId;
        return this;
    }

    @Nullable
    public String getConcernedIdentityId() {
        return concernedIdentityId;
    }

    public ProcessInstanceEventEntity setConcernedIdentityId(@Nullable String concernedIdentityId) {
        this.concernedIdentityId = concernedIdentityId;
        return this;
    }

    @Nullable
    public String getConcernedIdentityTitle() {
        return concernedIdentityTitle;
    }

    public ProcessInstanceEventEntity setConcernedIdentityTitle(@Nullable String concernedIdentityTitle) {
        this.concernedIdentityTitle = concernedIdentityTitle;
        return this;
    }

    // endregion
}
