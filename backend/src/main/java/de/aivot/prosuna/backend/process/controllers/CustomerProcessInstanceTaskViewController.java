package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.communication.services.IdentityCommunicationService;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.LinkButtonContentElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.elements.utils.ElementStreamUtils;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.identity.constants.IdentityQueryParameterConstants;
import de.aivot.prosuna.backend.identity.controllers.IdentityController;
import de.aivot.prosuna.backend.identity.dtos.EmailIdentityRequestDTO;
import de.aivot.prosuna.backend.identity.dtos.IdentityCommunicationSelectionRequestDTO;
import de.aivot.prosuna.backend.identity.dtos.IdentityProviderOptionResponseDTO;
import de.aivot.prosuna.backend.identity.dtos.IdentitySlotResponseDTO;
import de.aivot.prosuna.backend.identity.services.IdentitySlotService;
import de.aivot.prosuna.backend.identity.utils.IdentityCookieUtils;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.PaymentTaskRuntimeDataKeys;
import de.aivot.prosuna.backend.payment.services.PaymentTransactionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessTestClaimEntity;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceFilter;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceTaskFilter;
import de.aivot.prosuna.backend.process.models.ProcessNodeCustomerView;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.TaskViewEvent;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.services.*;
import de.aivot.prosuna.backend.process.workers.ProcessNodeExecutionResultHandler;
import de.aivot.prosuna.backend.services.PdfService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.exceptions.TemplateProcessingException;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/processes/{procAccess}/tasks/{taskAccess}/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance tasks."
)
public class CustomerProcessInstanceTaskViewController {
    private final ProcessInstanceService processInstanceService;
    private final ProcessInstanceTaskService processInstanceTaskService;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ProcessNodeService processDefinitionNodeService;
    private final ProcessNodeExecutionResultHandler processNodeExecutionResultHandler;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;
    private final ElementDerivationService elementDerivationService;
    private final FileUploadMultipartInputService fileUploadMultipartInputService;
    private final ProcessDataService processDataService;
    private final ProcessService processService;
    private final VDepartmentShadowedService vDepartmentShadowedService;
    private final PaymentTransactionService paymentTransactionService;
    private final PdfService pdfService;
    private final ProsunaConfig prosunaConfig;
    private final ThemeService themeService;
    private final AssetService assetService;
    private final CustomerTaskIdentityService customerTaskIdentityService;
    private final IdentitySlotService identitySlotService;

    public CustomerProcessInstanceTaskViewController(ProcessInstanceService processInstanceService,
                                                     ProcessInstanceTaskService processInstanceTaskService,
                                                     ProcessNodeDefinitionService processNodeProviderService,
                                                     ProcessNodeService processDefinitionNodeService,
                                                     ProcessNodeExecutionResultHandler processNodeExecutionResultHandler,
                                                     ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory,
                                                     ElementDerivationService elementDerivationService,
                                                     FileUploadMultipartInputService fileUploadMultipartInputService,
                                                     ProcessDataService processDataService,
                                                     ProcessService processService,
                                                     VDepartmentShadowedService vDepartmentShadowedService,
                                                     PaymentTransactionService paymentTransactionService,
                                                     PdfService pdfService,
                                                     ProsunaConfig prosunaConfig,
                                                     ThemeService themeService,
                                                     AssetService assetService,
                                                     CustomerTaskIdentityService customerTaskIdentityService,
                                                     IdentitySlotService identitySlotService) {
        this.processInstanceService = processInstanceService;
        this.processInstanceTaskService = processInstanceTaskService;
        this.processNodeProviderService = processNodeProviderService;
        this.processDefinitionNodeService = processDefinitionNodeService;
        this.processNodeExecutionResultHandler = processNodeExecutionResultHandler;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
        this.elementDerivationService = elementDerivationService;
        this.fileUploadMultipartInputService = fileUploadMultipartInputService;
        this.processDataService = processDataService;
        this.processService = processService;
        this.vDepartmentShadowedService = vDepartmentShadowedService;
        this.paymentTransactionService = paymentTransactionService;
        this.pdfService = pdfService;
        this.prosunaConfig = prosunaConfig;
        this.themeService = themeService;
        this.assetService = assetService;
        this.customerTaskIdentityService = customerTaskIdentityService;
        this.identitySlotService = identitySlotService;
    }

    @GetMapping("")
    @Operation(
            summary = "Retrieve Process Instance Task View Layout",
            description = "Retrieves the view layout for a specific task within a process instance. " +
                    "The layout defines how the task is presented to the user, including form fields and structure."
    )
    public <NodeConfig> TaskViewResponse retrieve(
            @Nonnull @PathVariable String procAccess,
            @Nonnull @PathVariable String taskAccess,
            @RequestParam(required = false) Map<String, List<String>> queryParameters,
            @Nullable @CookieValue(name = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId
    ) throws ResponseException {
        TaskViewData<NodeConfig> taskViewData = fetchTaskViewData(
                procAccess,
                taskAccess
        );

        var context = createCustomerContext(taskViewData, identitySessionId, queryParameters);

        var customerView = taskViewData
                .provider
                .getCustomerTaskView(context);
        return createTaskViewResponse(taskViewData, customerView, identitySessionId);
    }

    @GetMapping("identity/start/")
    @Operation(
            summary = "Start required customer identity authentication",
            description = "Redirects the customer to the identity provider required by the customer task view."
    )
    public <NodeConfig> void startRequiredIdentityAuthentication(
            @Nonnull @PathVariable String procAccess,
            @Nonnull @PathVariable String taskAccess,
            @Nonnull @RequestParam(name = IdentityQueryParameterConstants.ORIGIN) String origin,
            @RequestParam(required = false) Map<String, List<String>> queryParameters,
            @Nullable @CookieValue(name = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId,
            @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {
        TaskViewData<NodeConfig> taskViewData = fetchTaskViewData(procAccess, taskAccess);
        var context = createCustomerContext(
                taskViewData,
                identitySessionId,
                withoutIdentityOrigin(queryParameters)
        );
        var customerView = taskViewData
                .provider()
                .getCustomerTaskView(context);
        var redirectUri = customerTaskIdentityService.createAuthenticationRedirect(
                taskViewData.instance(),
                taskViewData.node(),
                customerView,
                identitySessionId,
                origin
        );

        response.sendRedirect(redirectUri.toString());
    }

    @GetMapping("identities/{identityId}/providers/{providerKey}/start/")
    @Operation(
            summary = "Start authentication for a new customer task identity",
            description = "Redirects the customer to a provider configured for the task's new identity slot."
    )
    public <NodeConfig> void startNewIdentityProviderAuthentication(
            @Nonnull @PathVariable String procAccess,
            @Nonnull @PathVariable String taskAccess,
            @Nonnull @PathVariable String identityId,
            @Nonnull @PathVariable UUID providerKey,
            @Nonnull @RequestParam(name = IdentityQueryParameterConstants.ORIGIN) String origin,
            @RequestParam(required = false) Map<String, List<String>> queryParameters,
            @Nullable @CookieValue(name = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId,
            @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {
        var taskViewData = fetchTaskViewData(procAccess, taskAccess);
        var context = createCustomerContext(
                taskViewData,
                identitySessionId,
                withoutIdentityOrigin(queryParameters)
        );
        var customerView = taskViewData.provider().getCustomerTaskView(context);
        var slot = customerTaskIdentityService.requireNewIdentitySlot(
                taskViewData.instance(),
                customerView,
                identityId
        );
        var redirectUri = identitySlotService.createAuthenticationRedirect(
                slot,
                identityId,
                providerKey,
                identitySessionId,
                origin,
                taskViewData.node().getId()
        );
        response.sendRedirect(redirectUri.toString());
    }

    @PutMapping("identities/{identityId}/email/")
    @Operation(summary = "Set the email address for a new customer task identity")
    public <NodeConfig> IdentitySlotResponseDTO setNewIdentityEmail(
            @Nonnull @PathVariable String procAccess,
            @Nonnull @PathVariable String taskAccess,
            @Nonnull @PathVariable String identityId,
            @RequestParam(required = false) Map<String, List<String>> queryParameters,
            @Nullable @CookieValue(name = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId,
            @Nonnull @Valid @RequestBody EmailIdentityRequestDTO request,
            @Nonnull HttpServletResponse response
    ) throws ResponseException {
        var taskViewData = fetchTaskViewData(procAccess, taskAccess);
        var context = createCustomerContext(taskViewData, identitySessionId, queryParameters);
        var customerView = taskViewData.provider().getCustomerTaskView(context);
        var slot = customerTaskIdentityService.requireNewIdentitySlot(
                taskViewData.instance(),
                customerView,
                identityId
        );
        var result = identitySlotService.setEmailIdentity(
                slot,
                identityId,
                identitySessionId,
                taskViewData.node().getId(),
                request.emailAddress()
        );
        response.addCookie(IdentityCookieUtils.createIdentityCookie(result.identitySessionId()));
        return result.slot();
    }

    @DeleteMapping("identities/{identityId}/")
    @Operation(summary = "Clear a new customer task identity")
    public <NodeConfig> void clearNewIdentity(
            @Nonnull @PathVariable String procAccess,
            @Nonnull @PathVariable String taskAccess,
            @Nonnull @PathVariable String identityId,
            @RequestParam(required = false) Map<String, List<String>> queryParameters,
            @Nullable @CookieValue(name = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId,
            @Nonnull HttpServletResponse response
    ) throws ResponseException {
        var taskViewData = fetchTaskViewData(procAccess, taskAccess);
        var context = createCustomerContext(taskViewData, identitySessionId, queryParameters);
        var customerView = taskViewData.provider().getCustomerTaskView(context);
        var slot = customerTaskIdentityService.requireNewIdentitySlot(
                taskViewData.instance(),
                customerView,
                identityId
        );
        var identitiesRemain = identitySlotService.clearIdentity(
                slot,
                identityId,
                identitySessionId,
                taskViewData.node().getId()
        );
        if (!identitiesRemain) {
            response.addCookie(IdentityCookieUtils.createExpiredIdentityCookie());
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @PutMapping("identities/{identityId}/communication/")
    @Operation(summary = "Select communication for a new customer task identity")
    public <NodeConfig> IdentityCommunicationService.SelectionState selectNewIdentityCommunication(
            @Nonnull @PathVariable String procAccess,
            @Nonnull @PathVariable String taskAccess,
            @Nonnull @PathVariable String identityId,
            @RequestParam(required = false) Map<String, List<String>> queryParameters,
            @Nonnull @CookieValue(name = IdentityController.IDENTITY_COOKIE_NAME) String identitySessionId,
            @Nonnull @Valid @RequestBody IdentityCommunicationSelectionRequestDTO request
    ) throws ResponseException {
        var taskViewData = fetchTaskViewData(procAccess, taskAccess);
        var context = createCustomerContext(taskViewData, identitySessionId, queryParameters);
        var customerView = taskViewData.provider().getCustomerTaskView(context);
        var slot = customerTaskIdentityService.requireNewIdentitySlot(
                taskViewData.instance(),
                customerView,
                identityId
        );
        return identitySlotService.selectCommunication(
                slot,
                identityId,
                identitySessionId,
                taskViewData.node().getId(),
                request.bindingId(),
                request.customerData()
        );
    }

    @PostMapping("identities/{identityId}/communication/derive/")
    @Operation(summary = "Preview communication for a new customer task identity")
    public <NodeConfig> IdentityCommunicationService.SelectionState deriveNewIdentityCommunication(
            @Nonnull @PathVariable String procAccess,
            @Nonnull @PathVariable String taskAccess,
            @Nonnull @PathVariable String identityId,
            @RequestParam(required = false) Map<String, List<String>> queryParameters,
            @Nonnull @CookieValue(name = IdentityController.IDENTITY_COOKIE_NAME) String identitySessionId,
            @Nonnull @Valid @RequestBody IdentityCommunicationSelectionRequestDTO request
    ) throws ResponseException {
        var taskViewData = fetchTaskViewData(procAccess, taskAccess);
        var context = createCustomerContext(taskViewData, identitySessionId, queryParameters);
        var customerView = taskViewData.provider().getCustomerTaskView(context);
        var slot = customerTaskIdentityService.requireNewIdentitySlot(
                taskViewData.instance(),
                customerView,
                identityId
        );
        return identitySlotService.previewCommunication(
                slot,
                identityId,
                identitySessionId,
                taskViewData.node().getId(),
                request.bindingId(),
                request.customerData()
        );
    }

    @GetMapping("payment-confirmation/")
    @Operation(
            summary = "Get process task payment confirmation PDF",
            description = "Downloads the payment confirmation PDF for a paid public process task."
    )
    public void getPaymentConfirmation(@Nonnull @PathVariable String procAccess,
                                       @Nonnull @PathVariable String taskAccess,
                                       @RequestParam(required = false) Map<String, List<String>> queryParameters,
                                       @Nullable @CookieValue(name = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId,
                                       @Nonnull HttpServletResponse response) throws ResponseException, IOException {
        var taskViewData = fetchTaskViewData(procAccess, taskAccess);
        var context = createCustomerContext(taskViewData, identitySessionId, queryParameters);
        var customerView = taskViewData
                .provider()
                .getCustomerTaskView(context);
        customerTaskIdentityService.requireAuthenticatedIdentity(
                taskViewData.instance(),
                taskViewData.node(),
                customerView,
                identitySessionId
        );
        var transaction = resolvePaymentConfirmationTransaction(taskViewData);

        if (transaction.getStatus() != XBezahldienstStatus.PAYED) {
            throw ResponseException.notFound();
        }

        var process = processService
                .retrieve(taskViewData.instance().getProcessId())
                .orElseThrow(ResponseException::notFound);
        var department = vDepartmentShadowedService
                .retrieve(process.getDepartmentId())
                .orElseThrow(() -> ResponseException.internalServerError("Keine zuständige Organisationseinheit für die Zahlungsbestätigung gefunden."));
        var logoUrl = resolvePaymentConfirmationLogoUrl(department);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generatePaymentConfirmation(
                    transaction,
                    taskViewData.instance().getCaseNumber(),
                    logoUrl,
                    department
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ResponseException.internalServerError(e, "Die PDF-Erstellung der Zahlungsbestätigung wurde unterbrochen.");
        } catch (IOException | URISyntaxException | TemplateProcessingException e) {
            throw ResponseException.internalServerError(e, "Fehler beim Erzeugen der Zahlungsbestätigung: %s", e.getMessage());
        }

        response.setContentType("application/pdf");
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition
                        .attachment()
                        .filename("Zahlungsbestaetigung-" + taskViewData.instance().getCaseNumber() + ".pdf", StandardCharsets.UTF_8)
                        .build()
                        .toString()
        );

        response.getOutputStream().write(pdfBytes);
    }

    @PutMapping("")
    @Operation(
            summary = "Retrieve Process Instance Task View Layout",
            description = "Retrieves the view layout for a specific task within a process instance. " +
                    "The layout defines how the task is presented to the user, including form fields and structure."
    )
    public <NodeConfig> TaskViewResponse update(
            @Nonnull @PathVariable String procAccess,
            @Nonnull @PathVariable String taskAccess,
            @RequestParam(value = "inputs", required = true) String rawInputs,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "fileUris", required = false) List<String> fileUris,
            @Nullable @RequestParam(value = "event", required = false) String rawEvent,
            @RequestParam(required = false) Map<String, List<String>> queryParameters,
            @Nullable @CookieValue(name = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId,
            @Nonnull HttpServletResponse response
    ) throws ResponseException {
        TaskViewData<NodeConfig> taskViewData = fetchTaskViewData(
                procAccess,
                taskAccess
        );

        var context = createCustomerContext(taskViewData, identitySessionId, queryParameters);
        var logger = context.getLogger();

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

        var customerView = taskViewData
                .provider
                .getCustomerTaskView(context);
        var identityState = customerTaskIdentityService.requireAuthenticatedIdentity(
                taskViewData.instance(),
                taskViewData.node(),
                customerView,
                identitySessionId
        );
        var layout = customerView.layout();

        var cleanEvent = resolveValidCustomerEvent(layout, customerView.events(), rawEvent);

        if (rawEvent != null && cleanEvent == null) {
            throw ResponseException.badRequest("Invalid event: " + rawEvent);
        }

        AuthoredElementValues inputs;
        try {
            inputs = JsonMapperFactory
                    .getInstance()
                    .readValue(rawInputs, AuthoredElementValues.class);
        } catch (JacksonException e) {
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
            return createTaskViewResponse(
                    identityState,
                    layout,
                    inputs,
                    customerView.events()
            );
        }

        var executionResult = res.get();
        var completingResult = customerTaskIdentityService.isCompletingResult(executionResult);
        var additionalIdentities = customerTaskIdentityService.getAdditionalIdentitiesForCompletion(
                identityState,
                executionResult
        );
        try {
            if (additionalIdentities.isEmpty()) {
                processNodeExecutionResultHandler.handleResult(
                        logger,
                        null,
                        taskViewData.provider,
                        taskViewData.node,
                        taskViewData.instance,
                        taskViewData.task,
                        previousTask,
                        executionResult
                );
            } else {
                processNodeExecutionResultHandler.handleResultWithAdditionalIdentities(
                            logger,
                            null,
                            taskViewData.provider,
                            taskViewData.node,
                            taskViewData.instance,
                            taskViewData.task,
                            previousTask,
                            executionResult,
                            additionalIdentities
                );
            }
        } catch (ProcessNodeExecutionException e) {
            logger.logException(e);
            throw ResponseException.internalServerError(e);
        }

        if (completingResult && identitySessionId != null) {
            var identitiesRemain = customerTaskIdentityService.clearTaskIdentitySession(
                    identitySessionId,
                    taskViewData.node()
            );
            if (!identitiesRemain) {
                response.addCookie(IdentityCookieUtils.createExpiredIdentityCookie());
            }
        }

        var updatedView = taskViewData
                .provider
                .getCustomerTaskView(context);
        var updatedIdentityState = completingResult
                ? identityState
                : customerTaskIdentityService.resolveIdentityState(
                taskViewData.instance(),
                taskViewData.node(),
                updatedView,
                identitySessionId
        );

        return createTaskViewResponse(
                updatedIdentityState,
                updatedView.layout(),
                updatedView.data(),
                updatedView.events()
        );
    }

    @Nullable
    private String resolveValidCustomerEvent(@Nonnull BaseElement layout,
                                             @Nonnull List<TaskViewEvent> events,
                                             @Nullable String rawEvent) {
        if (rawEvent == null) {
            return null;
        }

        var validEvents = new LinkedHashSet<String>();
        events.stream()
                .map(TaskViewEvent::event)
                .forEach(validEvents::add);
        addInlineCustomerEvents(layout, validEvents);

        return validEvents.contains(rawEvent) ? rawEvent : null;
    }

    private void addInlineCustomerEvents(@Nonnull BaseElement layout, @Nonnull Set<String> validEvents) {
        ElementStreamUtils.applyAction(layout, element -> {
            if (!(element instanceof LinkButtonContentElement linkButton)) {
                return;
            }

            var customerTaskEvent = linkButton.getCustomerTaskEvent();
            if (!hasValue(linkButton.getHref()) && hasValue(customerTaskEvent)) {
                validEvents.add(customerTaskEvent.trim());
            }
        });
    }

    private boolean hasValue(@Nullable String value) {
        return value != null && !value.isBlank();
    }

    @PostMapping("derive/")
    @Operation(
            summary = "Retrieve Process Instance Task View Layout",
            description = "Retrieves the view layout for a specific task within a process instance. " +
                    "The layout defines how the task is presented to the user, including form fields and structure."
    )
    public <NodeConfig> DerivedRuntimeElementData derive(
            @Nonnull @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String procAccess,
            @Nonnull @PathVariable String taskAccess,
            @Nonnull @RequestBody AuthoredElementValues authoredElementValues,
            @Nullable @RequestParam(value = "skipErrorsFor", required = false) List<String> skipErrorsFor,
            @RequestParam(required = false) Map<String, List<String>> queryParameters,
            @Nullable @CookieValue(name = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId
    ) throws ResponseException {
        TaskViewData<NodeConfig> taskViewData = fetchTaskViewData(
                procAccess,
                taskAccess
        );

        var incomingProcessExecutionData = processDataService
                .foldProcessInstanceData(
                        taskViewData.instance(),
                        taskViewData.task().getPreviousProcessNodeId(),
                        taskViewData.task()
                );

        var context = createCustomerContext(taskViewData, identitySessionId, queryParameters);

        var customerTaskView = taskViewData
                .provider
                .getCustomerTaskView(context);
        customerTaskIdentityService.requireAuthenticatedIdentity(
                taskViewData.instance(),
                taskViewData.node(),
                customerTaskView,
                identitySessionId
        );

        var elementDerivationRequest = new ElementDerivationRequest(
                customerTaskView.layout(),
                authoredElementValues,
                new ElementDerivationOptions()
                        .setSkipErrorsForElementIds(skipErrorsFor),
                incomingProcessExecutionData
        );

        return elementDerivationService
                .derive(elementDerivationRequest);
    }

    @Nonnull
    private <NodeConfig> TaskViewResponse createTaskViewResponse(
            @Nonnull TaskViewData<NodeConfig> taskViewData,
            @Nonnull ProcessNodeCustomerView customerView,
            @Nullable String identitySessionId
    ) throws ResponseException {
        var identityState = customerTaskIdentityService.resolveIdentityState(
                taskViewData.instance(),
                taskViewData.node(),
                customerView,
                identitySessionId
        );
        return createTaskViewResponse(
                identityState,
                customerView.layout(),
                customerView.data(),
                customerView.events()
        );
    }

    @Nonnull
    private TaskViewResponse createTaskViewResponse(
            @Nonnull CustomerTaskIdentityService.CustomerTaskIdentityState identityState,
            @Nonnull GroupLayoutElement layout,
            @Nonnull AuthoredElementValues data,
            @Nonnull List<TaskViewEvent> events
    ) {
        TaskViewExistingIdentitySlot existingIdentitySlot = null;
        if (identityState.existingIdentity() != null) {
            existingIdentitySlot = new TaskViewExistingIdentitySlot(
                    identityState.existingIdentity().id(),
                    identityState.existingIdentity().isReady(),
                    identityState.existingIdentity().identityProvider()
            );
        }

        return new TaskViewResponse(
                identityState.isReady() ? layout : null,
                identityState.isReady() ? data : null,
                identityState.isReady() ? events : null,
                identityState.newIdentitySlot(),
                existingIdentitySlot
        );
    }

    @Nonnull
    private Map<String, List<String>> withoutIdentityOrigin(
            @Nullable Map<String, List<String>> queryParameters
    ) {
        var customerQueryParameters = queryParameters == null
                ? new LinkedHashMap<String, List<String>>()
                : new LinkedHashMap<>(queryParameters);
        customerQueryParameters.remove(IdentityQueryParameterConstants.ORIGIN);
        return customerQueryParameters;
    }

    @Nonnull
    private <NodeConfig> ProcessNodeExecutionContextUICustomer<NodeConfig> createCustomerContext(
            @Nonnull TaskViewData<NodeConfig> taskViewData,
            @Nullable String identitySessionId,
            @Nullable Map<String, List<String>> queryParameters
    ) {
        var logger = processNodeExecutionLoggerFactory
                .create(
                        taskViewData.instance().getId(),
                        taskViewData.task().getId(),
                        null,
                        identitySessionId
                );

        return new ProcessNodeExecutionContextUICustomer<>(
                logger,
                taskViewData.node(),
                taskViewData.instance(),
                taskViewData.task(),
                new ProcessTestClaimEntity(), // TODO: Get Test Claim
                identitySessionId,
                taskViewData.nodeConfig(),
                queryParameters
        );
    }

    private <NodeConfig> TaskViewData<NodeConfig> fetchTaskViewData(
            @Nonnull String procAccess,
            @Nonnull String taskAccess
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

        var node = processDefinitionNodeService
                .retrieve(task.getProcessNodeId())
                .orElseThrow(ResponseException::notFound);

        var provider = (ProcessNodeDefinition<NodeConfig>) processNodeProviderService
                .getProcessNodeDefinition(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())
                .orElseThrow(ResponseException::notFound);

        var cfgRes = processDefinitionNodeService.deriveConfiguration(
                node,
                provider,
                null,
                true
        );

        return new TaskViewData<>(
                instance,
                task,
                node,
                provider,
                cfgRes.configuration()
        );
    }

    @Nonnull
    private PaymentTransactionEntity resolvePaymentConfirmationTransaction(@Nonnull TaskViewData<?> taskViewData) throws ResponseException {
        var transactionKey = taskViewData
                .task()
                .getRuntimeData()
                .get(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY);

        if (transactionKey == null) {
            throw ResponseException.notFound();
        }

        var transaction = paymentTransactionService
                .retrieve(String.valueOf(transactionKey))
                .orElseThrow(ResponseException::notFound);

        var expectedRedirectUrl = prosunaConfig.createUrl(
                "/process/",
                taskViewData.instance().getAccessKey(),
                "tasks",
                taskViewData.task().getAccessKey()
        );

        if (!Objects.equals(transaction.getRedirectUrl(), expectedRedirectUrl)) {
            throw ResponseException.notFound();
        }

        return transaction;
    }

    @Nonnull
    private String resolvePaymentConfirmationLogoUrl(@Nonnull VDepartmentShadowedEntity department) {
        UUID logoKey = null;
        if (department.getThemeId() != null) {
            logoKey = themeService
                    .retrieve(department.getThemeId())
                    .map(ThemeEntity::getLogoKey)
                    .orElse(null);
        }

        return logoKey == null ? prosunaConfig.getDefaultLogoUrl() : assetService.createUrl(logoKey);
    }

    private record TaskViewData<NodeConfig>(
            @Nonnull
            ProcessInstanceEntity instance,
            @Nonnull
            ProcessInstanceTaskEntity task,
            @Nonnull
            ProcessNodeEntity node,
            @Nonnull
            ProcessNodeDefinition<NodeConfig> provider,
            @Nonnull
            NodeConfig nodeConfig
    ) {

    }

    /**
     * A response object containing the layout, data, and events for a customer task view.
     * Configured identity requirements are included regardless of their current state. The response omits the task
     * content until every blocking identity requirement is ready.
     *
     * @param layout               The layout, or {@code null} while an identity requirement blocks access.
     * @param data                 The authored data, or {@code null} while an identity requirement blocks access.
     * @param events               The available events, or {@code null} while an identity requirement blocks access.
     * @param newIdentitySlot      The new identity slot, or {@code null} if none is configured.
     * @param existingIdentitySlot The existing provider identity requirement, or {@code null} if none applies.
     */
    public record TaskViewResponse(
            @Nullable
            GroupLayoutElement layout,
            @Nullable
            AuthoredElementValues data,
            @Nullable
            List<TaskViewEvent> events,
            @Nullable
            IdentitySlotResponseDTO newIdentitySlot,
            @Nullable
            TaskViewExistingIdentitySlot existingIdentitySlot
    ) {

    }

    public record TaskViewExistingIdentitySlot(
            @Nonnull
            String id,
            boolean isReady,
            @Nonnull
            IdentityProviderOptionResponseDTO identityProvider
    ) {

    }
}
