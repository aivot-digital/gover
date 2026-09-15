package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.services.CommunicationService;
import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.services.ProcessTaskMailService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.*;
import de.aivot.prosuna.backend.process.models.ProcessDataValueUtils;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import de.aivot.prosuna.backend.process.repositories.ProcessEdgeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ProcessNodeExecutionResultHandler {
    private final RabbitTemplate rabbitTemplate;
    private final CommunicationService communicationService;
    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final ProcessEdgeRepository processDefinitionEdgeRepository;
    private final UserService userService;
    private final ProcessTaskMailService processTaskMailService;
    private final ProcessNodeRepository processNodeRepository;
    private final ProcessNodeDefinitionService processNodeDefinitionService;
    private final ProcessService processService;
    private final DepartmentService departmentService;

    @Autowired
    public ProcessNodeExecutionResultHandler(RabbitTemplate rabbitTemplate,
                                             CommunicationService communicationService,
                                             ProcessInstanceRepository processInstanceRepository,
                                             ProcessInstanceTaskRepository processInstanceTaskRepository,
                                             ProcessEdgeRepository processDefinitionEdgeRepository,
                                             UserService userService,
                                             ProcessTaskMailService processTaskMailService,
                                             ProcessNodeRepository processNodeRepository,
                                             ProcessNodeDefinitionService processNodeDefinitionService,
                                             ProcessService processService,
                                             DepartmentService departmentService) {
        this.rabbitTemplate = rabbitTemplate;
        this.communicationService = communicationService;
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processDefinitionEdgeRepository = processDefinitionEdgeRepository;
        this.userService = userService;
        this.processTaskMailService = processTaskMailService;
        this.processNodeRepository = processNodeRepository;
        this.processNodeDefinitionService = processNodeDefinitionService;
        this.processService = processService;
        this.departmentService = departmentService;
    }

    public void handleResult(@Nonnull ProcessNodeExecutionLogger logger,
                             @Nullable UserEntity triggeringUser,
                             @Nonnull ProcessNodeDefinition<?> provider,
                             @Nonnull ProcessNodeEntity currentNode,
                             @Nonnull ProcessInstanceEntity processInstance,
                             @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                             @Nullable ProcessInstanceTaskEntity previousTask,
                             @Nullable ProcessNodeExecutionResult executionResult) throws ProcessNodeExecutionException {
        handleResultInternal(
                logger,
                triggeringUser,
                provider,
                currentNode,
                processInstance,
                processInstanceTask,
                previousTask,
                executionResult,
                Map.of()
        );
    }

    public void handleResultWithAdditionalIdentities(
            @Nonnull ProcessNodeExecutionLogger logger,
            @Nullable UserEntity triggeringUser,
            @Nonnull ProcessNodeDefinition<?> provider,
            @Nonnull ProcessNodeEntity currentNode,
            @Nonnull ProcessInstanceEntity processInstance,
            @Nonnull ProcessInstanceTaskEntity processInstanceTask,
            @Nullable ProcessInstanceTaskEntity previousTask,
            @Nullable ProcessNodeExecutionResult executionResult,
            @Nonnull Map<String, IdentityData> additionalIdentities
    ) throws ProcessNodeExecutionException {
        handleResultInternal(
                logger,
                triggeringUser,
                provider,
                currentNode,
                processInstance,
                processInstanceTask,
                previousTask,
                executionResult,
                additionalIdentities
        );
    }

    private void handleResultInternal(
            @Nonnull ProcessNodeExecutionLogger logger,
            @Nullable UserEntity triggeringUser,
            @Nonnull ProcessNodeDefinition<?> provider,
            @Nonnull ProcessNodeEntity currentNode,
            @Nonnull ProcessInstanceEntity processInstance,
            @Nonnull ProcessInstanceTaskEntity processInstanceTask,
            @Nullable ProcessInstanceTaskEntity previousTask,
            @Nullable ProcessNodeExecutionResult executionResult,
            @Nonnull Map<String, IdentityData> additionalIdentities
    ) throws ProcessNodeExecutionException {
        if (executionResult == null) {
            var err = String.format(
                    """
                            Der Prozesselement-Funktionsanbieter %s des Prozesselementes %s hat ein leeres Ergebnis zurückgegeben.
                            Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters!
                            """,
                    StringUtils.quote(provider.getName()),
                    StringUtils.quote(currentNode.resolveName(provider))
            );
            logger.logf(
                    ProcessNodeExecutionLogLevel.Error,
                    true,
                    true,
                    "Leeres Ergebnis",
                    err,
                    StringUtils.quote(currentNode.resolveName(provider))
            );
            throw new ProcessNodeExecutionExceptionBrokenImplementation(err)
                    .setAlreadyLogged(true);
        }

        validateAdditionalIdentities(
                processInstance,
                executionResult,
                additionalIdentities
        );

        var context = new HandlerContext<>(
                logger,
                triggeringUser,
                provider,
                currentNode,
                processInstance,
                processInstanceTask,
                previousTask,
                executionResult,
                additionalIdentities
        );

        handleCommunicationRequest(context);

        switch (executionResult) {
            case ProcessNodeExecutionResultPaymentRequested paymentRequested ->
                    handlePaymentRequested(context.withResult(paymentRequested));
            case ProcessNodeExecutionResultTaskUpdated taskUpdated ->
                    handleTaskUpdated(context.withResult(taskUpdated));
            case ProcessNodeExecutionResultTaskCompleted taskCompleted ->
                    handleTaskComplete(context.withResult(taskCompleted));
            case ProcessNodeExecutionResultInstanceCompleted instanceCompleted ->
                    handleInstanceComplete(context.withResult(instanceCompleted));
            case ProcessNodeExecutionResultTaskAssigned assigned -> handleAssigned(context.withResult(assigned));
            case ProcessNodeExecutionResultTaskAssignedCustomer assignedCustomer ->
                    handleAssignedCustomer(context.withResult(assignedCustomer));
            case ProcessNodeExecutionResultNoop ignored -> {
                // Do nothing here.
            }
            default -> throw new ProcessNodeExecutionExceptionBrokenImplementation(
                    """
                            Der Prozesselement-Funktionsanbieter „%s“ des Prozesselementes „%s“ hat eine unbekanntes Ergebnisklasse erzeugt: „%s“.
                            Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters!
                            """,
                    provider.getName(),
                    currentNode.resolveName(provider),
                    executionResult.getClass().getName()
            );
        }
    }

    private void validateAdditionalIdentities(@Nonnull ProcessInstanceEntity processInstance,
                                              @Nonnull ProcessNodeExecutionResult executionResult,
                                              @Nonnull Map<String, IdentityData> additionalIdentities)
            throws ProcessNodeExecutionExceptionBrokenImplementation {
        if (additionalIdentities.isEmpty()) {
            return;
        }
        if (!(executionResult instanceof ProcessNodeExecutionResultTaskCompleted)
                && !(executionResult instanceof ProcessNodeExecutionResultInstanceCompleted)) {
            throw new ProcessNodeExecutionExceptionBrokenImplementation(
                    "Neue Prozessidentitäten dürfen nur beim Abschluss einer Aufgabe oder eines Vorgangs übernommen werden."
            );
        }

        var processIdentities = processInstance.getIdentities();

        for (var entry : additionalIdentities.entrySet()) {
            var identity = entry.getValue();
            if (identity == null || !Objects.equals(entry.getKey(), identity.identityId())) {
                throw new ProcessNodeExecutionExceptionBrokenImplementation(
                        "Eine neue Prozessidentität besitzt keine konsistente Identitäts-ID."
                );
            }
            if (processIdentities != null && processIdentities.containsKey(entry.getKey())) {
                throw new ProcessNodeExecutionExceptionBrokenImplementation(
                        "Die neue Prozessidentität %s existiert bereits in der Prozessinstanz.",
                        StringUtils.quote(entry.getKey())
                );
            }
        }
    }

    private void applyAdditionalIdentities(@Nonnull ProcessInstanceEntity processInstance,
                                           @Nonnull Map<String, IdentityData> additionalIdentities) {
        if (additionalIdentities.isEmpty()) {
            return;
        }

        var processIdentities = processInstance.getIdentities();
        if (processIdentities == null) {
            processIdentities = new IdentityDataMap();
            processInstance.setIdentities(processIdentities);
        }
        processIdentities.putAll(additionalIdentities);
    }

    private void handleCommunicationRequest(@Nonnull HandlerContext<?> context) throws ProcessNodeExecutionException {
        var communicationRequest = context.result.getCommunicationRequest();
        if (communicationRequest == null) {
            return;
        }

        var processIdentities = context.processInstance.getIdentities();
        var recipientIdentity = processIdentities == null
                ? null
                : processIdentities.get(communicationRequest.recipientIdentityId());
        if (recipientIdentity == null) {
            recipientIdentity = context.additionalIdentities.get(communicationRequest.recipientIdentityId());
        }
        if (recipientIdentity == null) {
            markTaskFailed(context.processInstanceTask);
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die Empfängeridentität %s ist in der Prozessinstanz nicht vorhanden.",
                    StringUtils.quote(communicationRequest.recipientIdentityId())
            );
        }

        var message = communicationRequest.message().withSendingContext(
                context.triggeringUser,
                resolveSendingDepartment(context)
        );

        final Map<String, Object> sendResult;
        try {
            sendResult = communicationService.sendMessage(recipientIdentity, message);
        } catch (CommunicationException e) {
            markTaskFailed(context.processInstanceTask);
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die Nachricht an die Identität %s konnte nicht versendet werden: %s",
                    StringUtils.quote(communicationRequest.recipientIdentityId()),
                    e.getMessage()
            );
        }

        logCommunicationSent(context, recipientIdentity, message, sendResult);

        if (communicationRequest.nodeDataOutputKey() == null) {
            return;
        }

        var nodeData = context.result.getNodeData() == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<>(context.result.getNodeData());
        nodeData.put(communicationRequest.nodeDataOutputKey(), sendResult);
        context.result.setNodeData(nodeData);
    }

    private void logCommunicationSent(@Nonnull HandlerContext<?> context,
                                      @Nonnull IdentityData recipientIdentity,
                                      @Nonnull CommunicationMessage message,
                                      @Nonnull Map<String, Object> sendResult) {
        var processInstanceDetails = new LinkedHashMap<String, Object>();
        processInstanceDetails.put("id", context.processInstance.getId());
        processInstanceDetails.put("caseNumber", context.processInstance.getCaseNumber());
        processInstanceDetails.put("processId", context.processInstance.getProcessId());
        processInstanceDetails.put("initialProcessVersion", context.processInstance.getInitialProcessVersion());

        var recipientIdentityDetails = new LinkedHashMap<String, Object>();
        recipientIdentityDetails.put("identityId", recipientIdentity.identityId());
        recipientIdentityDetails.put("type", recipientIdentity.type());
        recipientIdentityDetails.put("providerKey", recipientIdentity.providerKey());
        recipientIdentityDetails.put("metadataIdentifier", recipientIdentity.metadataIdentifier());
        recipientIdentityDetails.put("emailAddress", recipientIdentity.emailAddress());
        recipientIdentityDetails.put("communicationProviderBindingId", recipientIdentity.communicationProviderBindingId());

        var messageDetails = new LinkedHashMap<String, Object>();
        messageDetails.put("subject", message.subject());
        messageDetails.put("body", message.body());
        messageDetails.put("htmlBody", message.htmlBody());
        messageDetails.put("sendingUser", createSendingUserDetails(message.sendingUser()));
        messageDetails.put("sendingDepartment", createSendingDepartmentDetails(message.sendingDepartment()));

        var eventDetails = new LinkedHashMap<String, Object>();
        eventDetails.put("processInstance", processInstanceDetails);
        eventDetails.put("recipientIdentity", recipientIdentityDetails);
        eventDetails.put("message", messageDetails);
        eventDetails.put("sendResult", sendResult);

        context.logger.logf(
                ProcessNodeExecutionLogLevel.Info,
                false,
                true,
                "Nachricht versendet",
                eventDetails,
                "Die Nachricht mit dem Betreff %s wurde erfolgreich an die Identität %s versendet.",
                StringUtils.quote(message.subject()),
                StringUtils.quote(recipientIdentity.identityId())
        );
    }

    @Nonnull
    private DepartmentEntity resolveSendingDepartment(@Nonnull HandlerContext<?> context)
            throws ProcessNodeExecutionException {
        final Optional<ProcessEntity> process;
        try {
            process = processService.retrieve(context.processInstance.getProcessId());
        } catch (ResponseException e) {
            markTaskFailed(context.processInstanceTask);
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Der Prozess %s der Prozessinstanz konnte nicht geladen werden: %s",
                    context.processInstance.getProcessId(),
                    e.getMessage()
            );
        }

        if (process.isEmpty()) {
            markTaskFailed(context.processInstanceTask);
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Prozess %s der Prozessinstanz ist nicht vorhanden.",
                    context.processInstance.getProcessId()
            );
        }

        var departmentId = process.get().getDepartmentId();
        if (departmentId == null) {
            markTaskFailed(context.processInstanceTask);
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Für den Prozess %s ist keine Organisationseinheit hinterlegt.",
                    process.get().getId()
            );
        }
        var department = departmentService.retrieve(departmentId);
        if (department.isEmpty()) {
            markTaskFailed(context.processInstanceTask);
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die Organisationseinheit %s des Prozesses ist nicht vorhanden.",
                    departmentId
            );
        }
        return department.get();
    }

    @Nullable
    private static Map<String, Object> createSendingUserDetails(@Nullable UserEntity user) {
        if (user == null) {
            return null;
        }
        var details = new LinkedHashMap<String, Object>();
        details.put("id", user.getId());
        details.put("name", user.getFullName());
        return details;
    }

    @Nullable
    private static Map<String, Object> createSendingDepartmentDetails(@Nullable DepartmentEntity department) {
        if (department == null) {
            return null;
        }
        var details = new LinkedHashMap<String, Object>();
        details.put("id", department.getId());
        details.put("name", department.getName());
        return details;
    }

    private void markTaskFailed(@Nonnull ProcessInstanceTaskEntity task) {
        task.setStatus(ProcessTaskStatus.Failed);
        task.setFinished(Instant.now());
        processInstanceTaskRepository.save(task);
    }

    private void handlePaymentRequested(@Nonnull HandlerContext<ProcessNodeExecutionResultPaymentRequested> context) {
        context.processInstanceTask.setStatus(ProcessTaskStatus.AwaitingPayment);
        assignAndSaveDataLayersAndStatusOverride(context, false);

        if (context.processInstance.getStatus() != ProcessInstanceStatus.Running) {
            context.processInstance.setStatus(ProcessInstanceStatus.Running);
            processInstanceRepository.save(context.processInstance);
        }

        context.logger.logf(
                ProcessNodeExecutionLogLevel.Info,
                false,
                true,
                "Zahlung angefordert",
                "Es wurde eine Zahlung über den Zahlungsanbieter %s mit der Transaktions-ID %s angefordert.",
                StringUtils.quote(context.result.getPaymentProviderName()),
                StringUtils.quote(context.result.getTransactionKey())
        );

        // TODO: Use communication package to send payment request information to target in a later product iteration.
    }

    private void handleAssigned(@Nonnull HandlerContext<ProcessNodeExecutionResultTaskAssigned> context) throws ProcessNodeExecutionException {
        String previousAssignedUserId = context.processInstanceTask.getAssignedUserId();

        UserEntity assignedUser;
        try {
            assignedUser = userService
                    .retrieve(context.result.getAssignedUserId())
                    .orElse(null);
        } catch (ResponseException e) {
            throw new ProcessNodeExecutionExceptionInvalidAssignment(
                    e,
                    """
                            Der Prozesselement-Funktionsanbieter „%s“ des Prozesselementes „%s“ hat eine ungültige Mitarbeiter:in-ID „%s“ zurückgegeben.
                            Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters!
                            """,
                    context.provider.getName(),
                    context.currentNode.resolveName(context.provider),
                    context.result.getAssignedUserId()
            );
        }

        if (assignedUser == null) {
            throw new ProcessNodeExecutionExceptionInvalidAssignment(
                    """
                            Der Prozesselement-Funktionsanbieter „%s“ des Prozesselementes „%s“ hat eine unbekannte Mitarbeiter:in-ID „%s“ zurückgegeben.
                            Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters!
                            """,
                    context.provider.getName(),
                    context.currentNode.resolveName(context.provider),
                    context.result.getAssignedUserId()
            );
        }

        context.processInstanceTask.setAssignedUserId(context.result.getAssignedUserId());
        assignAndSaveDataLayersAndStatusOverride(context, false);

        if (context.triggeringUser != null) {
            context.logger.logf(
                    ProcessNodeExecutionLogLevel.Info,
                    false,
                    true,
                    "Aufgabe neu zugewiesen",
                    "Die Aufgabe wurde durch %s der Mitarbeiter:in %s zugewiesen.",
                    StringUtils.quote(context.triggeringUser.getFullName()),
                    StringUtils.quote(assignedUser.getFullName())
            );
        } else {
            context.logger.logf(
                    ProcessNodeExecutionLogLevel.Info,
                    true,
                    true,
                    "Aufgabe " + StringUtils.quote(context.currentNode.resolveName(context.provider)) + " automatisch zugewiesen",
                    "Die Aufgabe wurde automatisch der Mitarbeiter:in %s zugewiesen.",
                    StringUtils.quote(assignedUser.getFullName())
            );
        }

        if (Objects.equals(previousAssignedUserId, assignedUser.getId())) {
            return;
        }

        if (context.triggeringUser != null && assignedUser.getId().equals(context.triggeringUser.getId())) {
            return;
        }

        if (context.processInstance.getStatus() != ProcessInstanceStatus.Running) {
            context.processInstance.setStatus(ProcessInstanceStatus.Running);
            processInstanceRepository.save(context.processInstance);
        }

        try {
            processTaskMailService.sendAssigned(
                    context.triggeringUser,
                    assignedUser,
                    context.processInstance,
                    context.processInstanceTask,
                    context.currentNode,
                    context.provider,
                    previousAssignedUserId != null
            );
        } catch (Exception e) {
            context.logger.logException(new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die E-Mail-Benachrichtigung für die zugewiesene Aufgabe an '%s' konnte nicht versendet werden.",
                    assignedUser.getFullName()
            ));
        }
    }


    private void handleAssignedCustomer(@Nonnull HandlerContext<ProcessNodeExecutionResultTaskAssignedCustomer> context) throws ProcessNodeExecutionException {
        String previousAssignedUserId = context.processInstanceTask.getAssignedCustomerIdentityId();

        final IdentityData assignedCustomer = context
                .processInstance()
                .getIdentities()
                .get(context.result.getIdentityId());


        if (assignedCustomer == null) {
            throw new ProcessNodeExecutionExceptionInvalidAssignment(
                    """
                            Der Prozesselement-Funktionsanbieter %s des Prozesselementes %s hat eine ungültige Identitäts-ID %s zurückgegeben.
                            Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters!
                            """,
                    StringUtils.quote(context.provider.getName()),
                    StringUtils.quote(context.currentNode.resolveName(context.provider)),
                    StringUtils.quote(context.result.getIdentityId())
            );
        }

        context.processInstanceTask.setAssignedCustomerIdentityId(assignedCustomer.identityId());
        context.processInstanceTask.setStatus(ProcessTaskStatus.AwaitingCustomer);

        assignAndSaveDataLayersAndStatusOverride(context, false);

        if (context.triggeringUser != null) {
            context.logger.logf(
                    ProcessNodeExecutionLogLevel.Info,
                    false,
                    true,
                    "Aufgabe neu zugewiesen",
                    "Die Aufgabe wurde durch %s der Identitäts-ID %s zugewiesen.",
                    StringUtils.quote(context.triggeringUser.getFullName()),
                    StringUtils.quote(assignedCustomer.identityId())
            );
        } else {
            context.logger.logf(
                    ProcessNodeExecutionLogLevel.Info,
                    true,
                    true,
                    "Aufgabe " + StringUtils.quote(context.currentNode.resolveName(context.provider)) + " automatisch zugewiesen",
                    "Die Aufgabe wurde automatisch der Identitäts-ID %s zugewiesen.",
                    StringUtils.quote(assignedCustomer.identityId())
            );
        }
    }

    private void handleTaskUpdated(@Nonnull HandlerContext<ProcessNodeExecutionResultTaskUpdated> context) throws ProcessNodeExecutionException {
        context.processInstanceTask.setStatus(ProcessTaskStatus.Running);
        assignAndSaveDataLayersAndStatusOverride(context, true);

        if (context.processInstance.getStatus() != ProcessInstanceStatus.Running) {
            context.processInstance.setStatus(ProcessInstanceStatus.Running);
            processInstanceRepository.save(context.processInstance);
        }

        if (context.triggeringUser != null) {
            context.logger.logf(
                    ProcessNodeExecutionLogLevel.Debug,
                    true,
                    false,
                    "Eingaben für " + StringUtils.quote(context.currentNode.resolveName(context.provider)) + " gespeichert",
                    "Für die Aufgabe %s wurden durch die Mitarbeiter:in %s Eingaben abgespeichert.",
                    StringUtils.quote(context.currentNode.resolveName(context.provider)),
                    StringUtils.quote(context.triggeringUser.getFullName())
            );
        } else {
            context.logger.logf(
                    ProcessNodeExecutionLogLevel.Debug,
                    true,
                    false,
                    "Eingaben für " + StringUtils.quote(context.currentNode.resolveName(context.provider)) + " gespeichert",
                    "Für die Aufgabe %s wurden durch die zugewiesen Mitarbeiter:in Eingaben abgespeichert.",
                    StringUtils.quote(context.currentNode.resolveName(context.provider))
            );
        }
    }

    private void handleTaskComplete(@Nonnull HandlerContext<ProcessNodeExecutionResultTaskCompleted> context) throws ProcessNodeExecutionException {
        var port = context
                .provider
                .getPorts()
                .stream()
                .filter(processNodePort -> processNodePort.key().equals(context.result.getViaPort()))
                .findFirst();

        if (port.isEmpty()) {
            throw new ProcessNodeExecutionExceptionBrokenImplementation(
                    """
                            Für das Prozesselement %s wird durch den Prozesselement-Funktionsanbieter %s kein ausgehender Port mit dem Schlüssel %s bereitgestellt.
                            Der Vorgang kann nicht fortgeführt werden. Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters.
                            """,
                    StringUtils.quote(context.currentNode.resolveName(context.provider)),
                    StringUtils.quote(context.provider.getName()),
                    StringUtils.quote(context.result.getViaPort())
            );
        }

        var outEdge = processDefinitionEdgeRepository
                .findByFromNodeIdAndViaPort(
                        context.currentNode.getId(),
                        context.result.getViaPort()
                );

        if (outEdge.isEmpty()) {
            throw new ProcessNodeExecutionExceptionBrokenImplementation(
                    """
                            Für das Prozesselement %s wurde kein ausgehender Pfad für den Port %s definiert.
                            Der Vorgang kann nicht fortgeführt werden. Bitte überprüfen Sie die Implementierung des Prozesselement-Funktionsanbieters.
                            Bitte prüfen Sie den Aufbau Ihres Prozessmodells.
                            """,
                    StringUtils.quote(context.currentNode.resolveName(context.provider)),
                    StringUtils.quote(port.get().label())
            );
        }

        context.processInstanceTask.setStatus(ProcessTaskStatus.Completed);
        context.processInstanceTask.setFinished(Instant.now());
        assignAndSaveDataLayersAndStatusOverride(context, true);

        applyAdditionalIdentities(context.processInstance, context.additionalIdentities);
        if (!context.additionalIdentities.isEmpty()
                || context.processInstance.getStatus() != ProcessInstanceStatus.Running) {
            context.processInstance.setStatus(ProcessInstanceStatus.Running);
            processInstanceRepository.save(context.processInstance);
        }

        var nextPayload = new ProcessWorker.DoWorkWorkerPayload(
                context.processInstance.getId(),
                context.processInstanceTask.getId(),
                context.currentNode.getId(),
                context.result.getViaPort(),
                outEdge.get().getToNodeId()
        );

        var nextNode = processNodeRepository
                .findById(outEdge.get().getToNodeId())
                .map(node -> processNodeDefinitionService
                        .getProcessNodeDefinition(node)
                        .map(node::resolveName)
                        .orElse("UNKNOWN"))
                .orElse("UNKNOWN");

        if (context.triggeringUser != null) {
            context.logger.logf(
                    ProcessNodeExecutionLogLevel.Info,
                    true,
                    true,
                    "Aufgabe " + StringUtils.quote(context.currentNode.resolveName(context.provider)) + " abgeschlossen",
                    """
                            Die Aufgabe für das Prozesselement %s wurde durch die Mitarbeiter:in %s abgeschlossen.
                            Als ausgehende Verbindung wird der Ausgang %s verwendet.
                            Das nächste Prozesselement ist %s.
                            """,
                    StringUtils.quote(context.currentNode.resolveName(context.provider)),
                    StringUtils.quote(context.triggeringUser.getFullName()),
                    StringUtils.quote(port.get().label()),
                    StringUtils.quote(nextNode)
            );
        } else {
            context.logger.logf(
                    ProcessNodeExecutionLogLevel.Info,
                    true,
                    true,
                    "Aufgabe " + StringUtils.quote(context.currentNode.resolveName(context.provider)) + " abgeschlossen",
                    """
                            Die Aufgabe für das Prozesselement %s wurde abgeschlossen.
                            Als ausgehende Verbindung wird der Ausgang %s verwendet.
                            Das nächste Prozesselement ist %s.
                            """,
                    StringUtils.quote(context.currentNode.resolveName(context.provider)),
                    StringUtils.quote(port.get().label()),
                    StringUtils.quote(nextNode)
            );
        }

        rabbitTemplate.convertAndSend(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE, nextPayload);
    }

    private void handleInstanceComplete(@Nonnull HandlerContext<ProcessNodeExecutionResultInstanceCompleted> context) {
        var completionTime = Instant.now();

        context.processInstanceTask.setStatus(ProcessTaskStatus.Completed);
        context.processInstanceTask.setFinished(completionTime);
        assignAndSaveDataLayersAndStatusOverride(context, true);

        applyAdditionalIdentities(context.processInstance, context.additionalIdentities);
        context.processInstance.setStatus(ProcessInstanceStatus.Completed);
        context.processInstance.setFinished(completionTime);
        context.processInstance.setKeepUntil(context.result.getRetentionDate());
        processInstanceRepository.save(context.processInstance);

        context.logger.logf(
                ProcessNodeExecutionLogLevel.Info,
                true,
                true,
                "Vorgang abgeschlossen",
                "Der Vorgang wurde erfolgreich abgeschlossen. Das abschließende Prozesselement war %s",
                StringUtils.quote(context.currentNode.resolveName(context.provider))
        );
    }

    private void assignAndSaveDataLayersAndStatusOverride(@Nonnull HandlerContext<?> context,
                                                          boolean applyOutputMappings) {
        var newRuntimeData = context.result.getRuntimeData();
        if (newRuntimeData == null) {
            newRuntimeData = new HashMap<>();
        }
        context.processInstanceTask.setRuntimeData(newRuntimeData);

        var newNodeData = context.result.getNodeData();
        if (newNodeData == null) {
            newNodeData = new HashMap<>();
        }
        context.processInstanceTask.setNodeData(newNodeData);

        var newProcessData = context.result.getProcessData();
        if (newProcessData == null) {
            newProcessData = context.previousTask != null ?
                    context.previousTask.getProcessData() :
                    context.processInstance.getInitialPayload();
        }
        if (applyOutputMappings) {
            context.processInstanceTask.setProcessData(applyOutputMappings(
                    context.provider,
                    context.currentNode.getOutputMappings(),
                    newNodeData,
                    newProcessData
            ));
        } else {
            context.processInstanceTask.setProcessData(newProcessData);
        }

        // Apply task status overrides
        if (context.result.getTaskStatusOverride() != null) {
            context.processInstanceTask.setStatusOverride(context.result.getTaskStatusOverride());
        }
        if (Boolean.TRUE.equals(context.result.getClearTaskStatusOverride())) {
            context.processInstanceTask.setStatusOverride(null);
        }

        processInstanceTaskRepository.save(context.processInstanceTask);
    }

    private static <NodeConfig> Map<String, Object> applyOutputMappings(@Nonnull ProcessNodeDefinition<NodeConfig> provider,
                                                                        @Nonnull Map<String, String> outputMappings,
                                                                        @Nonnull Map<String, Object> nodeData,
                                                                        @Nonnull Map<String, Object> processData) {
        var executionData = new ProcessExecutionData()
                .addProcessData(new HashMap<>(processData));

        for (var nodeProviderOutput : provider.getOutputs()) {
            var targetFieldPath = StringUtils.toNullableTrimmedString(outputMappings.get(nodeProviderOutput.key()));
            if (targetFieldPath == null) {
                continue;
            }

            var outputValue = nodeData.get(nodeProviderOutput.key());
            try {
                if (ProcessDataValueUtils.hasWildcardSegment(targetFieldPath)) {
                    var wildcardBindings = ProcessDataValueUtils
                            .resolveMatchingProcessDataValues(executionData, targetFieldPath)
                            .stream()
                            .map(ProcessDataValueUtils.ResolvedProcessDataValue::wildcardIndices)
                            .toList();

                    for (var wildcardBinding : wildcardBindings) {
                        ProcessDataValueUtils.writeProcessDataValue(
                                executionData,
                                targetFieldPath,
                                outputValue,
                                wildcardBinding
                        );
                    }
                } else {
                    ProcessDataValueUtils.writeProcessDataValue(
                            executionData,
                            targetFieldPath,
                            outputValue
                    );
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new IllegalStateException(
                        "Das Ausgabe-Mapping '%s' fuer den Knotenausgang '%s' ist ungueltig: %s"
                                .formatted(targetFieldPath, nodeProviderOutput.key(), e.getMessage()),
                        e
                );
            }
        }

        return executionData.getProcessData();
    }

    private record HandlerContext<T extends ProcessNodeExecutionResult>(
            @Nonnull ProcessNodeExecutionLogger logger,
            @Nullable UserEntity triggeringUser,
            @Nonnull ProcessNodeDefinition<?> provider,
            @Nonnull ProcessNodeEntity currentNode,
            @Nonnull ProcessInstanceEntity processInstance,
            @Nonnull ProcessInstanceTaskEntity processInstanceTask,
            @Nullable ProcessInstanceTaskEntity previousTask,
            @Nonnull T result,
            @Nonnull Map<String, IdentityData> additionalIdentities
    ) {
        public <S extends ProcessNodeExecutionResult> HandlerContext<S> withResult(@Nonnull S newResult) {
            return new HandlerContext<>(
                    logger,
                    triggeringUser,
                    provider,
                    currentNode,
                    processInstance,
                    processInstanceTask,
                    previousTask,
                    newResult,
                    additionalIdentities
            );
        }
    }
}
