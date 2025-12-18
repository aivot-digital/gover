package de.aivot.GoverBackend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

@Entity
@Table(name = "process_instance_history_events")
public class ProcessInstanceHistoryEventEntity {
    private static final String ID_SEQUENCE_NAME = "process_instance_history_events_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Long id;

    private java.time.LocalDateTime timestamp;

    @Length(max = 36)
    private String triggeringUserId;

    @Column(name = "process_instance_id")
    private Long processInstanceId;

    @Column(name = "process_instance_task_id")
    private Long processInstanceTaskId;

    // region Getters and Setters

    public Long getId() {
        return id;
    }

    public ProcessInstanceHistoryEventEntity setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public ProcessInstanceHistoryEventEntity setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public @Length(max = 36) String getTriggeringUserId() {
        return triggeringUserId;
    }

    public ProcessInstanceHistoryEventEntity setTriggeringUserId(@Length(max = 36) String triggeringUserId) {
        this.triggeringUserId = triggeringUserId;
        return this;
    }

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public ProcessInstanceHistoryEventEntity setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    public Long getProcessInstanceTaskId() {
        return processInstanceTaskId;
    }

    public ProcessInstanceHistoryEventEntity setProcessInstanceTaskId(Long processInstanceTaskId) {
        this.processInstanceTaskId = processInstanceTaskId;
        return this;
    }

    // endregion
}