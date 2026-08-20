package de.aivot.prosuna.backend.process.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.identity.controllers.IdentityController;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessTestClaimEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceFilter;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceTaskFilter;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.TaskViewEvent;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.services.*;
import de.aivot.prosuna.backend.process.workers.ProcessNodeExecutionResultHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final ProcessNodeService processDefinitionNodeService;
    private final ProcessNodeExecutionResultHandler processNodeExecutionResultHandler;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;
    private final ElementDerivationService elementDerivationService;
    private final FileUploadMultipartInputService fileUploadMultipartInputService;

    public CitizenProcessInstanceTaskViewController(ProcessInstanceService processInstanceService,
                                                    ProcessInstanceTaskService processInstanceTaskService,
                                                    ProcessNodeDefinitionService processNodeProviderService,
                                                    ProcessNodeService processDefinitionNodeService,
                                                    ProcessNodeExecutionResultHandler processNodeExecutionResultHandler,
                                                    ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory,
                                                    ElementDerivationService elementDerivationService,
                                                    FileUploadMultipartInputService fileUploadMultipartInputService) {
        this.processInstanceService = processInstanceService;
        this.processInstanceTaskService = processInstanceTaskService;
        this.processNodeProviderService = processNodeProviderService;
        this.processDefinitionNodeService = processDefinitionNodeService;
        this.processNodeExecutionResultHandler = processNodeExecutionResultHandler;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
        this.elementDerivationService = elementDerivationService;
        this.fileUploadMultipartInputService = fileUploadMultipartInputService;
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
            @Nullable @RequestHeader(name = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId
    ) throws ResponseException {
        var taskViewData = fetchTaskViewData(
                procAccess,
                taskAccess
        );

        var logger = processNodeExecutionLoggerFactory
                .create(
                        taskViewData.instance.getId(),
                        taskViewData.task.getId(),
                        null,
                        identitySessionId
                );

        var context = new ProcessNodeExecutionContextUICustomer(
                logger,
                taskViewData.node,
                taskViewData.instance,
                taskViewData.task,
                new ProcessTestClaimEntity(), // TODO: Get Test Claim
                identitySessionId
        );

        var layout = taskViewData
                .provider
                .getCustomerTaskView(context);

        var events = taskViewData
                .provider
                .getCustomerTaskViewEvents(context);

        var elementData = taskViewData
                .provider
                .getCustomerTaskViewData(context);

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
            @RequestParam(value = "inputs", required = true) String rawInputs,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "fileUris", required = false) List<String> fileUris,
            @Nullable @RequestParam(value = "event", required = false) String rawEvent,
            @Nullable @RequestHeader(name = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId
    ) throws ResponseException {
        var taskViewData = fetchTaskViewData(
                procAccess,
                taskAccess
        );

        var logger = processNodeExecutionLoggerFactory
                .create(
                        taskViewData.instance.getId(),
                        taskViewData.task.getId(),
                        null,
                        identitySessionId
                );

        var context = new ProcessNodeExecutionContextUICustomer(
                logger,
                taskViewData.node,
                taskViewData.instance,
                taskViewData.task,
                new ProcessTestClaimEntity(), // TODO: Get Test Claim
                identitySessionId
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
                .getCustomerTaskView(context);

        var events = taskViewData
                .provider
                .getCustomerTaskViewEvents(context);

        // Test if the event is valid
        var cleanEvent = events
                .stream()
                .filter(e -> e.event().equals(rawEvent))
                .findFirst()
                .map(TaskViewEvent::event)
                .orElse(null);

        if (rawEvent != null && cleanEvent == null) {
            throw ResponseException.badRequest("Invalid event: " + rawEvent);
        }

        AuthoredElementValues inputs;
        try {
            inputs = ObjectMapperFactory
                    .getInstance()
                    .readValue(rawInputs, AuthoredElementValues.class);
        } catch (JsonProcessingException e) {
            throw ResponseException.badRequest("Ungültige Eingabedaten.", e);
        }
        inputs = fileUploadMultipartInputService.normalizeInputs(
                layout,
                inputs,
                files,
                fileUris,
                taskViewData.instance.getId(),
                taskViewData.task.getId(),
                null
        ).inputs();

        var derivedElementData = elementDerivationService.derive(
                new ElementDerivationRequest(
                        layout,
                        inputs,
                        new ElementDerivationOptions()
                )
        );

        if (derivedElementData.hasAnyError()) {
            throw ResponseException.badRequest("Es ist ein Fehler beim Ableiten der Eingabedaten aufgetreten. Bitte überprüfen Sie Ihre Eingaben.", derivedElementData);
        }

        Optional<ProcessNodeExecutionResult> res;
        try {
            if (cleanEvent == null) {
                res = taskViewData
                        .provider
                        .onAutoSaveFromCustomerTaskView(
                                context,
                                inputs,
                                derivedElementData
                        );
            } else {
                res = taskViewData
                        .provider
                        .onEventFromCustomerTaskView(
                                context,
                                inputs,
                                derivedElementData,
                                cleanEvent
                        );
            }
        } catch (Exception e) {
            logger.logException(e);
            throw ResponseException.internalServerError(e);
        }

        if (res.isEmpty()) {
            return new TaskViewResponse(
                    layout,
                    inputs,
                    events
            );
        }

        try {
            processNodeExecutionResultHandler
                    .handleResult(
                            logger,
                            null,
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
                .getCustomerTaskView(context);

        var updatedEvents = taskViewData
                .provider
                .getCustomerTaskViewEvents(context);

        var updatedElementData = taskViewData
                .provider
                .getCustomerTaskViewData(context);

        return new TaskViewResponse(
                updatedLayout,
                updatedElementData,
                updatedEvents
        );
    }

    private <NodeConfig> TaskViewData<NodeConfig> fetchTaskViewData(
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

        var provider = (ProcessNodeDefinition<NodeConfig>) processNodeProviderService
                .getProcessNodeDefinition(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())
                .orElseThrow(ResponseException::notFound);

        return new TaskViewData<>(
                instance,
                task,
                node,
                provider
        );
    }

    private record TaskViewData<NodeConfig>(
            @Nonnull
            ProcessInstanceEntity instance,
            @Nonnull
            ProcessInstanceTaskEntity task,
            @Nonnull
            ProcessNodeEntity node,
            @Nonnull
            ProcessNodeDefinition<NodeConfig> provider
    ) {

    }

    public record TaskViewResponse(
            @Nonnull
            GroupLayoutElement layout,
            @Nonnull
            AuthoredElementValues data,
            @Nonnull
            List<TaskViewEvent> events
    ) {
    }
}
