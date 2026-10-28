package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.ProcessTaskDeadlineExceededData;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessWorkRequestService;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;

@Service
public class ProcessTaskDeadlineResultHandler {
    private final ProcessInstanceTaskRepository tasks;
    private final ProcessWorkRequestService workRequests;
    private final ProcessInstanceAttachmentService attachments;

    public ProcessTaskDeadlineResultHandler(@Nonnull ProcessInstanceTaskRepository tasks,
                                             @Nonnull ProcessWorkRequestService workRequests,
                                             @Nonnull ProcessInstanceAttachmentService attachments) {
        this.tasks = tasks;
        this.workRequests = workRequests;
        this.attachments = attachments;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void handle(@Nonnull ProcessInstanceTaskEntity task, @Nonnull ProcessNodeDeadlineDefinition definition,
                       @Nonnull ProcessEdgeEntity edge, @Nonnull Instant now, @Nonnull ProcessNodeExecutionLogger logger) {
        var data = new ProcessTaskDeadlineExceededData(definition.key(), definition.label(), task.getDeadline(), now);
        var nodeData = new LinkedHashMap<>(task.getNodeData());
        nodeData.put(ProcessTaskDeadlineExceededData.NODE_DATA_KEY, data.toMap());
        var summary = "Die Frist „%s“ bis %s wurde überschritten. Die Aufgabe wurde beendet. Der Vorgang wird über den Ausgang „%s“ fortgesetzt."
                .formatted(ExecutionSummaryMarkdown.text(definition.label()), ExecutionSummaryMarkdown.timestamp(task.getDeadline()),
                        ExecutionSummaryMarkdown.text(definition.port().label()));
        task.setNodeData(nodeData).setStatus(ProcessTaskStatus.DeadlineExceeded).setStatusOverride(null)
                .setFinished(now).setExecutionSummaryMarkdown(summary);
        tasks.save(task);
        attachments.deleteRemovedUploadsOfTask(task, null);
        workRequests.enqueueTransition(task.getProcessInstanceId(), task.getId(), edge.getViaPort(), edge.getToNodeId());
        var details = new LinkedHashMap<>(data.toMap());
        details.put("viaPort", edge.getViaPort());
        logger.logf(ProcessNodeExecutionLogLevel.Warn, false, true, true, task.getAssignedUserId(),
                task.getAssignedCustomerIdentityId(), null, "Frist überschritten", details, "%s", summary);
    }
}
