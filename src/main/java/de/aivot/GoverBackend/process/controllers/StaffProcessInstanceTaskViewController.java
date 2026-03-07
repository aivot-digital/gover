package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.identity.controllers.IdentityController;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.filters.ProcessInstanceTaskFilter;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextUIStaff;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.TaskViewEvent;
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

@RestController
@RequestMapping("/api/processes/{procId}/tasks/{taskId}/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance tasks."
)
public class StaffProcessInstanceTaskViewController {
    private final ProcessInstanceService processInstanceService;
    private final ProcessInstanceTaskService processInstanceTaskService;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ProcessNodeService processDefinitionNodeService;
    private final ProcessNodeExecutionResultHandler processNodeExecutionResultHandler;
    private final ProcessDataService processDataService;
    private final UserService userService;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;

    public StaffProcessInstanceTaskViewController(ProcessInstanceService processInstanceService,
                                                  ProcessInstanceTaskService processInstanceTaskService,
                                                  ProcessNodeDefinitionService processNodeProviderService,
                                                  ProcessNodeService processDefinitionNodeService,
                                                  ProcessNodeExecutionResultHandler processNodeExecutionResultHandler,
                                                  ProcessDataService processDataService,
                                                  UserService userService, ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory) {
        this.processInstanceService = processInstanceService;
        this.processInstanceTaskService = processInstanceTaskService;
        this.processNodeProviderService = processNodeProviderService;
        this.processDefinitionNodeService = processDefinitionNodeService;
        this.processNodeExecutionResultHandler = processNodeExecutionResultHandler;
        this.processDataService = processDataService;
        this.userService = userService;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
    }

    @GetMapping("")
    @Operation(
            summary = "Retrieve Process Instance Task View Layout",
            description = "Retrieves the view layout for a specific task within a process instance. " +
                    "The layout defines how the task is presented to the user, including form fields and structure."
    )
    public TaskViewResponse retrieve(
            @Nonnull @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long procId,
            @Nonnull @PathVariable Long taskId
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var taskViewData = fetchTaskViewData(
                jwt,
                procId,
                taskId
        );

        var logger = processNodeExecutionLoggerFactory
                .create(taskViewData.instance().getId(), taskViewData.task().getId(), user.getId(), null);

        var context = new ProcessNodeExecutionContextUIStaff(logger,
                taskViewData.node(),
                taskViewData.instance(),
                taskViewData.task(),
                null,
                user
        );

        var layout = taskViewData
                .provider
                .getStaffTaskView(context);

        var events = taskViewData
                .provider
                .getStaffTaskViewEvents(context);

        var elementData = taskViewData
                .provider
                .getStaffTaskViewData(context);

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
            @Nonnull @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long procId,
            @Nonnull @PathVariable Long taskId,
            @Nonnull @RequestBody ElementData elementData,
            @Nullable @RequestParam(value = "event", required = true) String event,
            @Nullable @RequestHeader(name = IdentityController.IDENTITY_HEADER_NAME, required = false) String identityId
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var taskViewData = fetchTaskViewData(
                jwt,
                procId,
                taskId
        );

        var logger = processNodeExecutionLoggerFactory
                .create(taskViewData.instance().getId(), taskViewData.task().getId(), user.getId(), identityId);

        var context = new ProcessNodeExecutionContextUIStaff(logger,
                taskViewData.node(),
                taskViewData.instance(),
                taskViewData.task(),
                null,
                user
        );

        ProcessInstanceTaskEntity previousTask;
        if (taskViewData.task.getPreviousProcessNodeId() != null) {
            previousTask = processInstanceTaskService
                    .retrieve(
                            ProcessInstanceTaskFilter
                                    .create()
                                    .setProcessInstanceId(taskViewData.instance.getId())
                                    .setProcessNodeId(taskViewData.task.getPreviousProcessNodeId())
                                    .build()
                    )
                    .orElse(null);
        } else {
            previousTask = null;
        }

        var layout = taskViewData
                .provider
                .getStaffTaskView(context);

        var events = taskViewData
                .provider
                .getStaffTaskViewEvents(context);

        // Test if the event is valid
        events
                .stream()
                .filter(e -> e.event().equals(event))
                .findFirst()
                .orElseThrow(() -> ResponseException.badRequest("Invalid event: " + event));

        var valueMap = ElementData
                .toValueMap((BaseElement) layout, elementData);

        var processData = processDataService
                .foldProcessInstanceData(
                        taskViewData.instance,
                        taskViewData.task.getPreviousProcessNodeId()
                );

        Optional<ProcessNodeExecutionResult> res;
        try {
            res = taskViewData
                    .provider
                    .onUpdateFromStaff(context, valueMap, event);
        } catch (Exception e) {
            logger.logException(e);
            throw ResponseException.internalServerError(e);
        }

        if (res.isEmpty()) {
            var workingElementData = ElementData
                    .fromValueMap((BaseElement) layout, taskViewData.task.getProcessData());
            return new TaskViewResponse(
                    layout,
                    workingElementData,
                    events
            );
        }

        try {
            processNodeExecutionResultHandler
                    .handleResult(
                            logger,
                            user,
                            taskViewData.provider,
                            taskViewData.node,
                            taskViewData.instance,
                            taskViewData.task,
                            previousTask,
                            res.get()
                    );
        } catch (ProcessNodeExecutionException e) {
            logger.logException(e);
            throw ResponseException.internalServerError(e);
        }

        var updatedLayout = taskViewData
                .provider
                .getStaffTaskView(context);

        var updatedEvents = taskViewData
                .provider
                .getStaffTaskViewEvents(context);

        var updatedElementData = taskViewData
                .provider
                .getStaffTaskViewData(context);

        return new TaskViewResponse(
                updatedLayout,
                updatedElementData,
                updatedEvents
        );
    }

    private TaskViewData fetchTaskViewData(
            @Nonnull Jwt jwt,
            @Nonnull Long procId,
            @Nonnull Long taskId
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var instance = processInstanceService
                .retrieve(procId)
                .orElseThrow(ResponseException::notFound);

        var task = processInstanceTaskService
                .retrieve(taskId)
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
                user,
                instance,
                task,
                node,
                provider
        );
    }

    private record TaskViewData(
            @Nonnull
            UserEntity user,
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
            LayoutElement<?> layout,
            @Nonnull
            ElementData data,
            @Nonnull
            List<TaskViewEvent> events
    ) {

    }
}
