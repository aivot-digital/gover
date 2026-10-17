package de.aivot.prosuna.backend.process.models.processContext;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Read-only input for generating a task's execution summary. The configuration is the one used for the completing
 * execution; it is not derived again. The task exposes the final runtime, node and mapped process data, status and
 * completion time. The instance includes identities added by this completion.
 *
 * @param configurationOfExecutingNode The effective typed configuration used for execution.
 * @param thisNode The executing node.
 * @param thisProcessInstance The owning process instance.
 * @param thisTask The completed task, before it is saved with its summary.
 * @param previousTask The preceding task, or {@code null} for an initial task.
 * @param triggeringUser The staff user triggering completion, or {@code null} for automatic or customer execution.
 * @param viaPort The selected outgoing port, or {@code null} when completing the whole instance.
 */
public record ProcessNodeExecutionSummaryContext<NodeConfig>(
        @Nonnull NodeConfig configurationOfExecutingNode,
        @Nonnull ProcessNodeEntity thisNode,
        @Nonnull ProcessInstanceEntity thisProcessInstance,
        @Nonnull ProcessInstanceTaskEntity thisTask,
        @Nullable ProcessInstanceTaskEntity previousTask,
        @Nullable UserEntity triggeringUser,
        @Nullable String viaPort
) {
}
