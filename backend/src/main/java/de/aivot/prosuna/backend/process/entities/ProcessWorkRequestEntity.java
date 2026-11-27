package de.aivot.prosuna.backend.process.entities;

import de.aivot.prosuna.backend.process.converters.ProcessWorkRequestKindConverter;
import de.aivot.prosuna.backend.process.enums.ProcessWorkRequestKind;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Durable outbox entry for pending process work.
 *
 * <p>The entry is committed together with the state change that requires the work. It is the only record of that
 * work: RabbitMQ only carries the entry's ID, so lost messages or an empty broker cannot lose a step. The ID is
 * published after the commit and published again if the entry is still pending after the redispatch interval.
 * Entries of paused or failed instances are not published until the instance runs again.
 *
 * <p>The consumer locks the owning process instance, rechecks this entry and the task the work would create, and
 * runs the work and deletes this entry in the same transaction. These checks make duplicate deliveries safe.
 * Entries for paused or failed instances remain pending; entries for completed or aborted instances are discarded.
 */
@Entity
@Table(name = "process_work_requests")
public class ProcessWorkRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "process_work_requests_id_seq")
    @SequenceGenerator(name = "process_work_requests_id_seq", allocationSize = 1)
    @Nullable
    private Long id;
    @Nonnull
    @Convert(converter = ProcessWorkRequestKindConverter.class)
    private ProcessWorkRequestKind kind;
    @Nonnull
    private Long processInstanceId;
    @Nullable
    private Long taskId;
    @Nullable
    @Column(length = 96)
    private String viaPort;
    @Nullable
    private Integer toNodeId;
    @Nonnull
    private Instant created;
    @Nullable
    private Instant lastDispatched;

    protected ProcessWorkRequestEntity() {
    }

    private ProcessWorkRequestEntity(@Nonnull ProcessWorkRequestKind kind, @Nonnull Long processInstanceId,
                                     @Nullable Long taskId, @Nullable String viaPort, @Nullable Integer toNodeId,
                                     @Nonnull Instant created) {
        this.kind = kind;
        this.processInstanceId = processInstanceId;
        this.taskId = taskId;
        this.viaPort = viaPort;
        this.toNodeId = toNodeId;
        this.created = created;
        // The ID is published after the commit, so the dispatcher only republishes it after the redispatch interval.
        this.lastDispatched = created;
    }

    @Nonnull
    public static ProcessWorkRequestEntity start(@Nonnull Long processInstanceId, @Nonnull Instant created) {
        return new ProcessWorkRequestEntity(ProcessWorkRequestKind.Start, processInstanceId, null, null, null, created);
    }

    @Nonnull
    public static ProcessWorkRequestEntity transition(@Nonnull Long processInstanceId, @Nonnull Long predecessorTaskId,
                                                      @Nonnull String viaPort, @Nonnull Integer toNodeId,
                                                      @Nonnull Instant created) {
        return new ProcessWorkRequestEntity(ProcessWorkRequestKind.Transition, processInstanceId, predecessorTaskId,
                viaPort, toNodeId, created);
    }

    @Nonnull
    public static ProcessWorkRequestEntity restart(@Nonnull Long processInstanceId, @Nonnull Long failedTaskId,
                                                   @Nonnull Instant created) {
        return new ProcessWorkRequestEntity(ProcessWorkRequestKind.Restart, processInstanceId, failedTaskId, null, null,
                created);
    }

    @Nonnull
    public static ProcessWorkRequestEntity resume(@Nonnull Long processInstanceId, @Nonnull Long taskId,
                                                  @Nonnull Instant created) {
        return new ProcessWorkRequestEntity(ProcessWorkRequestKind.Resume, processInstanceId, taskId, null, null, created);
    }

    @Nullable public Long getId() { return id; }
    @Nonnull public ProcessWorkRequestKind getKind() { return kind; }
    @Nonnull public Long getProcessInstanceId() { return processInstanceId; }
    @Nullable public Long getTaskId() { return taskId; }
    @Nullable public String getViaPort() { return viaPort; }
    @Nullable public Integer getToNodeId() { return toNodeId; }
    @Nonnull public Instant getCreated() { return created; }
    @Nullable public Instant getLastDispatched() { return lastDispatched; }
}
