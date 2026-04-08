package de.aivot.GoverBackend.process.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.identity.controllers.IdentityController;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntityId;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
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
import org.springframework.web.multipart.MultipartFile;

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
    private final UserService userService;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;
    private final ElementDerivationService elementDerivationService;
    private final ProcessService processService;
    private final ProcessVersionService processVersionService;
    private final TaskViewMultipartInputService taskViewMultipartInputService;
    private final ProcessDataService processDataService;

    public StaffProcessInstanceTaskViewController(ProcessInstanceService processInstanceService,
                                                  ProcessInstanceTaskService processInstanceTaskService,
                                                  ProcessNodeDefinitionService processNodeProviderService,
                                                  ProcessNodeService processDefinitionNodeService,
                                                  ProcessNodeExecutionResultHandler processNodeExecutionResultHandler,
                                                  UserService userService,
                                                  ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory,
                                                  ElementDerivationService elementDerivationService,
                                                  ProcessService processService,
                                                  ProcessVersionService processVersionService,
                                                  TaskViewMultipartInputService taskViewMultipartInputService, ProcessDataService processDataService) {
        this.processInstanceService = processInstanceService;
        this.processInstanceTaskService = processInstanceTaskService;
        this.processNodeProviderService = processNodeProviderService;
        this.processDefinitionNodeService = processDefinitionNodeService;
        this.processNodeExecutionResultHandler = processNodeExecutionResultHandler;
        this.userService = userService;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
        this.elementDerivationService = elementDerivationService;
        this.processService = processService;
        this.processVersionService = processVersionService;
        this.taskViewMultipartInputService = taskViewMultipartInputService;
        this.processDataService = processDataService;
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

        var processData = processDataService
                .foldProcessInstanceData(
                        taskViewData.instance(),
                        taskViewData.task().getPreviousProcessNodeId()
                );

        var context = new ProcessNodeExecutionContextUIStaff(
                logger,
                taskViewData.node(),
                taskViewData.instance(),
                taskViewData.task(),
                null,
                user,
                taskViewData.derivedRuntimeElementData(),
                processData
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
            @RequestParam(value = "inputs", required = true) String rawInputs,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "fileUris", required = false) List<String> fileUris,
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

        var processData = processDataService
                .foldProcessInstanceData(
                        taskViewData.instance(),
                        taskViewData.task().getPreviousProcessNodeId()
                );

        var context = new ProcessNodeExecutionContextUIStaff(
                logger,
                taskViewData.node(),
                taskViewData.instance(),
                taskViewData.task(),
                null,
                user,
                taskViewData.derivedRuntimeElementData(),
                processData
        );

        ProcessInstanceTaskEntity previousTask;
        if (taskViewData.task.getPreviousProcessNodeId() != null) {
            previousTask = processInstanceTaskService
                    .retrieveLatestForInstanceIdAndNodeId(
                            taskViewData.instance.getId(),
                            taskViewData.task.getPreviousProcessNodeId()
                    )
                    .orElse(null);
        } else {
            previousTask = null;
        }

        var events = taskViewData
                .provider
                .getStaffTaskViewEvents(context);

        // Test if the event is valid
        events
                .stream()
                .filter(e -> e.event().equals(event))
                .findFirst()
                .orElseThrow(() -> ResponseException.badRequest("Invalid event: " + event));

        AuthoredElementValues inputs;
        try {
            inputs = ObjectMapperFactory
                    .getInstance()
                    .readValue(rawInputs, AuthoredElementValues.class);
        } catch (JsonProcessingException e) {
            throw ResponseException.badRequest("Ungültige Eingabedaten.", e);
        }
        inputs = taskViewMultipartInputService.normalizeInputs(
                inputs,
                files,
                fileUris,
                taskViewData.instance().getId(),
                taskViewData.task().getId(),
                user.getId()
        );

        Optional<ProcessNodeExecutionResult> res;
        try {
            res = taskViewData
                    .provider
                    .onUpdateFromStaff(context, inputs, event);
        } catch (ResponseException e) {
            throw e;
        } catch (Exception e) {
            logger.logException(e);
            throw ResponseException.internalServerError(e);
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

        var process = processService
                .retrieve(node.getProcessId())
                .orElseThrow(ResponseException::notFound);

        var version = processVersionService
                .retrieve(ProcessVersionEntityId.of(node.getProcessId(), node.getProcessVersion()))
                .orElseThrow(ResponseException::notFound);

        var provider = processNodeProviderService
                .getProcessNodeDefinition(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())
                .orElseThrow(ResponseException::notFound);

        var configContext = new ProcessNodeDefinitionContextConfig(
                user,
                process,
                version,
                node
        );
        var derivationRequest = new ElementDerivationRequest(
                provider.getConfigurationLayout(configContext),
                node.getConfiguration(),
                new ElementDerivationOptions()
        );
        var derivationLogger = new ElementDerivationLogger();
        var derivedRuntimeData = elementDerivationService
                .derive(derivationRequest, derivationLogger);

        return new TaskViewData(
                user,
                instance,
                task,
                node,
                provider,
                derivedRuntimeData
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
            ProcessNodeDefinition provider,
            @Nonnull
            DerivedRuntimeElementData derivedRuntimeElementData
    ) {

    }

    public record TaskViewResponse(
            @Nonnull
            LayoutElement<?> layout,
            @Nonnull
            AuthoredElementValues data,
            @Nonnull
            List<TaskViewEvent> events
    ) {

    }
}
