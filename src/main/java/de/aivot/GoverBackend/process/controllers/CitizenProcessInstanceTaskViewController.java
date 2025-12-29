package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.identity.controllers.IdentityController;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.filters.ProcessInstanceFilter;
import de.aivot.GoverBackend.process.filters.ProcessInstanceTaskFilter;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.TaskViewEvent;
import de.aivot.GoverBackend.process.services.*;
import de.aivot.GoverBackend.process.workers.ProcessNodeExecutionResultHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/processes/{procAccess}/tasks/{taskAccess}/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance tasks."
)
public class CitizenProcessInstanceTaskViewController {
    private final ProcessInstanceService processInstanceService;
    private final ProcessInstanceTaskService processInstanceTaskService;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ProcessDefinitionNodeService processDefinitionNodeService;
    private final ProcessNodeExecutionResultHandler processNodeExecutionResultHandler;
    private final ProcessDataService processDataService;

    public CitizenProcessInstanceTaskViewController(ProcessInstanceService processInstanceService,
                                                    ProcessInstanceTaskService processInstanceTaskService,
                                                    ProcessNodeDefinitionService processNodeProviderService,
                                                    ProcessDefinitionNodeService processDefinitionNodeService,
                                                    ProcessNodeExecutionResultHandler processNodeExecutionResultHandler,
                                                    ProcessDataService processDataService) {
        this.processInstanceService = processInstanceService;
        this.processInstanceTaskService = processInstanceTaskService;
        this.processNodeProviderService = processNodeProviderService;
        this.processDefinitionNodeService = processDefinitionNodeService;
        this.processNodeExecutionResultHandler = processNodeExecutionResultHandler;
        this.processDataService = processDataService;
    }

    @GetMapping("")
    @Operation(
            summary = "Retrieve Process Instance Task View Layout",
            description = "Retrieves the view layout for a specific task within a process instance. " +
                    "The layout defines how the task is presented to the user, including form fields and structure."
    )
    public TaskViewResponse retrieve(
            @Nonnull @PathVariable UUID procAccess,
            @Nonnull @PathVariable UUID taskAccess,
            @Nullable @RequestHeader(name = IdentityController.IDENTITY_HEADER_NAME, required = false) String identityId
    ) throws ResponseException {
        var taskViewData = fetchTaskViewData(
                procAccess,
                taskAccess
        );

        var layout = taskViewData
                .provider
                .getCustomerTaskView(
                        identityId,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task
                );

        var events = taskViewData
                .provider
                .getCustomerTaskViewEvents(
                        identityId,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task
                );

        var elementData = taskViewData
                .provider
                .getCustomerTaskViewData(
                        identityId,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task
                );

        return new TaskViewResponse(
                layout,
                elementData,
                events
        );
    }

    @PutMapping("")
    @Operation(
            summary = "Retrieve Process Instance Task View Layout",
            description = "Retrieves the view layout for a specific task within a process instance. " +
                    "The layout defines how the task is presented to the user, including form fields and structure."
    )
    public TaskViewResponse update(
            @Nonnull @PathVariable UUID procAccess,
            @Nonnull @PathVariable UUID taskAccess,
            @Nonnull @RequestBody ElementData elementData,
            @Nullable @RequestParam(value = "event", required = true) String event,
            @Nullable @RequestHeader(name = IdentityController.IDENTITY_HEADER_NAME, required = false) String identityId
    ) throws ResponseException {
        var taskViewData = fetchTaskViewData(
                procAccess,
                taskAccess
        );

        ProcessInstanceTaskEntity previousTask;
        if (taskViewData.task.getPreviousProcessNodeId() != null) {
            previousTask = processInstanceTaskService
                    .retrieve(
                            ProcessInstanceTaskFilter
                                    .create()
                                    .setProcessInstanceId(taskViewData.instance.getId())
                                    .setProcessDefinitionNodeId(taskViewData.task.getPreviousProcessNodeId())
                                    .build()
                    )
                    .orElse(null);
        } else {
            previousTask = null;
        }

        var layout = taskViewData
                .provider
                .getCustomerTaskView(
                        identityId,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task
                );

        var events = taskViewData
                .provider
                .getCustomerTaskViewEvents(
                        identityId,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task
                );

        // Test if the event is valid
        events
                .stream()
                .filter(e -> e.event().equals(event))
                .findFirst()
                .orElseThrow(() -> ResponseException.badRequest("Invalid event: " + event));

        var valueMap = ElementData
                .toValueMap(layout, elementData);

        var processData = processDataService
                .foldProcessInstanceData(
                        taskViewData.instance,
                        taskViewData.task.getPreviousProcessNodeId()
                );

        Optional<ProcessNodeExecutionResult> res;
        try {
            res = taskViewData
                    .provider
                    .updateCustomer(
                            identityId,
                            taskViewData.instance,
                            taskViewData.node,
                            processData,
                            valueMap,
                            event
                    );
        } catch (Exception e) {
            throw ResponseException.internalServerError(e);
        }

        if (res.isEmpty()) {
            var workingElementData = ElementData
                    .fromValueMap(layout, taskViewData.task.getProcessData());
            return new TaskViewResponse(
                    layout,
                    workingElementData,
                    events
            );
        }

        processNodeExecutionResultHandler
                .handleResult(
                        null,
                        taskViewData.provider,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task,
                        previousTask,
                        res.get()
                );

        var updatedLayout = taskViewData
                .provider
                .getCustomerTaskView(
                        identityId,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task
                );

        var updatedEvents = taskViewData
                .provider
                .getCustomerTaskViewEvents(
                        identityId,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task
                );

        var updatedElementData = taskViewData
                .provider
                .getCustomerTaskViewData(
                        identityId,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task
                );

        return new TaskViewResponse(
                updatedLayout,
                updatedElementData,
                updatedEvents
        );
    }

    private TaskViewData fetchTaskViewData(
            @Nonnull UUID procAccess,
            @Nonnull UUID taskAccess
    ) throws ResponseException {
        var instance = processInstanceService
                .retrieve(ProcessInstanceFilter
                        .create()
                        .setAccessKey(procAccess)
                        .build()
                )
                .orElseThrow(ResponseException::notFound);

        var task = processInstanceTaskService
                .retrieve(ProcessInstanceTaskFilter
                        .create()
                        .setProcessInstanceId(instance.getId())
                        .setAccessKey(taskAccess)
                        .build()
                )
                .orElseThrow(ResponseException::notFound);

        if (task.getStatus() != ProcessTaskStatus.Running) {
            throw ResponseException.forbidden();
        }

        var node = processDefinitionNodeService
                .retrieve(task.getProcessNodeId())
                .orElseThrow(ResponseException::notFound);

        var provider = processNodeProviderService
                .getProcessNodeDefinition(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())
                .orElseThrow(ResponseException::notFound);

        return new TaskViewData(
                instance,
                task,
                node,
                provider
        );
    }

    private record TaskViewData(
            @Nonnull
            ProcessInstanceEntity instance,
            @Nonnull
            ProcessInstanceTaskEntity task,
            @Nonnull
            ProcessNodeEntity node,
            @Nonnull
            ProcessNodeDefinition provider
    ) {

    }

    public record TaskViewResponse(
            @Nonnull
            GroupLayoutElement layout,
            @Nonnull
            ElementData data,
            @Nonnull
            List<TaskViewEvent> events
    ) {

    }
}
