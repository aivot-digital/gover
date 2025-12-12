package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.identity.controllers.IdentityController;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.filters.ProcessInstanceFilter;
import de.aivot.GoverBackend.process.filters.ProcessInstanceTaskFilter;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.services.*;
import de.aivot.GoverBackend.process.workers.ProcessNodeExecutionResultHandler;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/processes/{procAccess}/task/{taskAccess}/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance tasks."
)
public class ProcessInstanceTaskViewController {
    private final ProcessInstanceService processInstanceService;
    private final ProcessInstanceTaskService processInstanceTaskService;
    private final ProcessNodeProviderService processNodeProviderService;
    private final ProcessDefinitionNodeService processDefinitionNodeService;
    private final UserService userService;
    private final ProcessNodeExecutionResultHandler processNodeExecutionResultHandler;
    private final ProcessDataService processDataService;

    public ProcessInstanceTaskViewController(ProcessInstanceService processInstanceService,
                                             ProcessInstanceTaskService processInstanceTaskService,
                                             ProcessNodeProviderService processNodeProviderService,
                                             ProcessDefinitionNodeService processDefinitionNodeService,
                                             UserService userService, ProcessNodeExecutionResultHandler processNodeExecutionResultHandler, ProcessDataService processDataService) {
        this.processInstanceService = processInstanceService;
        this.processInstanceTaskService = processInstanceTaskService;
        this.processNodeProviderService = processNodeProviderService;
        this.processDefinitionNodeService = processDefinitionNodeService;
        this.userService = userService;
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
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID procAccess,
            @Nonnull @PathVariable UUID taskAccess
    ) throws ResponseException {
        var taskViewData = fetchTaskViewData(
                jwt,
                procAccess,
                taskAccess
        );

        var layout = taskViewData
                .provider
                .getTaskViewLayout(
                        taskViewData.user,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task
                );

        var events = taskViewData
                .provider
                .getTaskViewEvents(
                        taskViewData.user,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task
                );

        var elementData = ElementData
                .fromValueMap(layout, taskViewData.task.getWorkingData());

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
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID procAccess,
            @Nonnull @PathVariable UUID taskAccess,
            @Nonnull @RequestBody ElementData elementData,
            @Nullable @RequestParam(value = "event", required = true) String event,
            @Nullable @RequestHeader(name = IdentityController.IDENTITY_HEADER_NAME, required = false) UUID identityId
    ) throws ResponseException {
        var taskViewData = fetchTaskViewData(
                jwt,
                procAccess,
                taskAccess
        );

        ProcessInstanceTaskEntity previousTask;
        if (taskViewData.task.getPreviousProcessDefinitionNodeId() != null) {
            previousTask = processInstanceTaskService
                    .retrieve(
                            ProcessInstanceTaskFilter
                                    .create()
                                    .setProcessInstanceId(taskViewData.instance.getId())
                                    .setProcessDefinitionNodeId(taskViewData.task.getPreviousProcessDefinitionNodeId())
                                    .build()
                    )
                    .orElse(null);
        } else {
            previousTask = null;
        }

        var layout = taskViewData
                .provider
                .getTaskViewLayout(
                        taskViewData.user,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task
                );

        var events = taskViewData
                .provider
                .getTaskViewEvents(
                        taskViewData.user,
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
                        taskViewData.task.getPreviousProcessDefinitionNodeId()
                );

        Optional<ProcessNodeExecutionResult> res;
        try {
            res = taskViewData
                    .provider
                    .update(
                            taskViewData.user,
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
                    .fromValueMap(layout, taskViewData.task.getWorkingData());
            return new TaskViewResponse(
                    layout,
                    workingElementData,
                    events
            );
        }

        processNodeExecutionResultHandler
                .handleResult(
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task,
                        previousTask,
                        res.get()
                );

        var updatedLayout = taskViewData
                .provider
                .getTaskViewLayout(
                        taskViewData.user,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task
                );

        var updatedEvents = taskViewData
                .provider
                .getTaskViewEvents(
                        taskViewData.user,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task
                );

        var updatedElementData = ElementData
                .fromValueMap(updatedLayout, taskViewData.task.getWorkingData());

        return new TaskViewResponse(
                updatedLayout,
                updatedElementData,
                updatedEvents
        );
    }

    private TaskViewData fetchTaskViewData(
            @Nullable Jwt jwt,
            @Nonnull UUID procAccess,
            @Nonnull UUID taskAccess
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElse(null);

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
                .retrieve(task.getProcessDefinitionNodeId())
                .orElseThrow(ResponseException::notFound);

        var provider = processNodeProviderService
                .getProcessNodeProvider(node.getCodeKey())
                .orElseThrow(ResponseException::notFound);

        var canAccess = provider
                .canGetTaskViewLayout(user, node, instance, task);

        if (!canAccess) {
            throw ResponseException.forbidden();
        }

        return new TaskViewData(
                user,
                instance,
                task,
                node,
                provider
        );
    }

    private record TaskViewData(
            @Nullable
            UserEntity user,
            @Nonnull
            ProcessInstanceEntity instance,
            @Nonnull
            ProcessInstanceTaskEntity task,
            @Nonnull
            ProcessDefinitionNodeEntity node,
            @Nonnull
            ProcessNodeProvider provider
    ) {

    }

    public record TaskViewResponse(
            @Nonnull
            GroupLayout layout,
            @Nonnull
            ElementData data,
            @Nonnull
            List<TaskViewEvent> events
    ) {

    }
}
