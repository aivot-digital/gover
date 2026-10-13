package de.aivot.prosuna.backend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Durable outbox entry for continuing a process after a hard task deadline expires.
 *
 * <p>The entry is committed together with the task's {@code DeadlineExceeded} status and history event.
 * It keeps the next step recoverable across RabbitMQ outages and application restarts. A worker publishes the
 * entry's ID and publishes it again only if the entry is still pending after the redispatch interval. Entries
 * of paused or failed instances are not published until the instance runs again.
 *
 * <p>The consumer locks the owning process instance, rechecks this entry and any existing child task,
 * and creates the next task and deletes this entry in the same transaction. These checks make duplicate
 * deliveries safe. Entries for paused or failed instances remain pending; entries for completed or
 * aborted instances are discarded.
 */
@Entity
@Table(name = "process_task_deadline_continuations")
public class ProcessTaskDeadlineContinuationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "process_task_deadline_continuations_id_seq")
    @SequenceGenerator(name = "process_task_deadline_continuations_id_seq", allocationSize = 1)
    @Nullable
    private Long id;
    @Nonnull
    private Long processInstanceId;
    @Nonnull
    private Long taskId;
    @Nonnull
    private Integer fromNodeId;
    @Nonnull
    private Integer toNodeId;
    @Nonnull
    @Column(length = 96)
    private String viaPort;
    @Nonnull
    private Instant created;
    @Nullable
    private Instant lastDispatched;

    protected ProcessTaskDeadlineContinuationEntity() {
    }

    public ProcessTaskDeadlineContinuationEntity(@Nonnull Long processInstanceId, @Nonnull Long taskId,
                                                 @Nonnull Integer fromNodeId, @Nonnull Integer toNodeId,
                                                 @Nonnull String viaPort, @Nonnull Instant created) {
        this.processInstanceId = processInstanceId;
        this.taskId = taskId;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.viaPort = viaPort;
        this.created = created;
    }

    @Nullable public Long getId() { return id; }
    @Nonnull public Long getProcessInstanceId() { return processInstanceId; }
    @Nonnull public Long getTaskId() { return taskId; }
    @Nonnull public Integer getFromNodeId() { return fromNodeId; }
    @Nonnull public Integer getToNodeId() { return toNodeId; }
    @Nonnull public String getViaPort() { return viaPort; }
    @Nonnull public Instant getCreated() { return created; }
    @Nullable public Instant getLastDispatched() { return lastDispatched; }
}
