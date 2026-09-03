package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.services.CommunicationService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.services.ProcessTaskMailService;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultCommunicationRequest;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultPaymentRequested;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskUpdated;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.repositories.ProcessEdgeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessNodeExecutionResultHandlerTest {
    @Test
    void handleResult_DispatchesCommunicationAndMapsProviderResultBeforeOutputs() throws Exception {
        var communicationService = mock(CommunicationService.class);
        var identity = providerIdentity("applicant");
        var message = CommunicationMessage.of("Subject", "Body", "Body");
        var sendResult = Map.<String, Object>of(
                "submissionId", "submission-1",
                "status", "SUBMITTED"
        );
        when(communicationService.sendMessage(same(identity), same(message))).thenReturn(sendResult);

        var savedTasks = new ArrayList<ProcessInstanceTaskEntity>();
        var handler = createHandler(
                savedTasks,
                Map.of(),
                new RecordingProcessTaskMailService(),
                communicationService
        );
        var task = processInstanceTask(null);
        var logger = new RecordingProcessNodeExecutionLogger();
        var processInstance = processInstance(identity);

        handler.handleResult(
                logger,
                null,
                new TestProcessNodeDefinition("Fallback task", List.of(
                        new ProcessNodeOutput("sendResult", "Send result", "Provider result", "Record<string, unknown>")
                )),
                processNode("Nachricht", Map.of("sendResult", "delivery")),
                processInstance,
                task,
                null,
                new ProcessNodeExecutionResultTaskUpdated()
                        .setNodeData(Map.of("existing", true))
                        .setProcessData(Map.of())
                        .setCommunicationRequest(new ProcessNodeExecutionResultCommunicationRequest(
                                identity.identityId(),
                                message,
                                "sendResult"
                        ))
        );

        verify(communicationService).sendMessage(same(identity), same(message));
        assertEquals(sendResult, task.getNodeData().get("sendResult"));
        assertEquals(Map.of("delivery", sendResult), task.getProcessData());
        assertEquals(ProcessTaskStatus.Running, task.getStatus());
        assertEquals(1, savedTasks.size());

        var event = logger.events.stream()
                .filter(candidate -> candidate.title().equals("Nachricht versendet"))
                .findFirst()
                .orElseThrow();
        assertEquals(ProcessNodeExecutionLogLevel.Info, event.level());
        assertFalse(event.technical());
        assertEquals(true, event.auditable());
        assertEquals(
                "Die Nachricht mit dem Betreff „Subject“ wurde erfolgreich an die Identität „applicant“ versendet.",
                event.message()
        );

        assertEquals(Map.of(
                "id", processInstance.getId(),
                "caseNumber", processInstance.getCaseNumber(),
                "processId", processInstance.getProcessId(),
                "initialProcessVersion", processInstance.getInitialProcessVersion()
        ), event.details().get("processInstance"));

        @SuppressWarnings("unchecked")
        var identityDetails = (Map<String, Object>) event.details().get("recipientIdentity");
        assertEquals(identity.identityId(), identityDetails.get("identityId"));
        assertEquals(identity.type(), identityDetails.get("type"));
        assertEquals(identity.providerKey(), identityDetails.get("providerKey"));
        assertEquals(identity.metadataIdentifier(), identityDetails.get("metadataIdentifier"));
        assertEquals(identity.emailAddress(), identityDetails.get("emailAddress"));
        assertEquals(identity.communicationProviderBindingId(), identityDetails.get("communicationProviderBindingId"));
        assertFalse(identityDetails.containsKey("sessionId"));
        assertFalse(identityDetails.containsKey("attributes"));
        assertFalse(identityDetails.containsKey("communicationProviderData"));

        assertEquals(Map.of(
                "subject", "Subject",
                "body", "Body",
                "htmlBody", "Body"
        ), event.details().get("message"));
        assertEquals(sendResult, event.details().get("sendResult"));
    }

    @Test
    void handleResult_AppliesPaymentRequestedOnlyAfterCommunicationSucceeds() throws Exception {
        var communicationService = mock(CommunicationService.class);
        var identity = identity("applicant");
        var message = CommunicationMessage.of("Payment", "Please pay", "Please pay");
        when(communicationService.sendMessage(same(identity), same(message))).thenReturn(Map.of());

        var savedTasks = new ArrayList<ProcessInstanceTaskEntity>();
        var handler = createHandler(
                savedTasks,
                Map.of(),
                new RecordingProcessTaskMailService(),
                communicationService
        );
        var task = processInstanceTask(null);
        var logger = new RecordingProcessNodeExecutionLogger();

        handler.handleResult(
                logger,
                null,
                new TestProcessNodeDefinition("Payment"),
                processNode("Payment"),
                processInstance(identity),
                task,
                null,
                new ProcessNodeExecutionResultPaymentRequested("transaction-1", "Provider")
                        .setRuntimeData(Map.of("transactionKey", "transaction-1"))
                        .setCommunicationRequest(new ProcessNodeExecutionResultCommunicationRequest(
                                identity.identityId(),
                                message,
                                null
                        ))
        );

        verify(communicationService).sendMessage(same(identity), same(message));
        assertEquals(ProcessTaskStatus.AwaitingPayment, task.getStatus());
        assertEquals("transaction-1", task.getRuntimeData().get("transactionKey"));
        assertEquals(1, savedTasks.size());
        assertEquals(1, logger.events.stream()
                .filter(event -> event.title().equals("Nachricht versendet"))
                .count());
    }

    @Test
    void handleResult_MarksTaskFailedWhenCommunicationFails() throws Exception {
        var communicationService = mock(CommunicationService.class);
        var identity = identity("applicant");
        var message = CommunicationMessage.of("Subject", "Body", "Body");
        when(communicationService.sendMessage(same(identity), same(message)))
                .thenThrow(new CommunicationException("Versand fehlgeschlagen"));

        var savedTasks = new ArrayList<ProcessInstanceTaskEntity>();
        var handler = createHandler(
                savedTasks,
                Map.of(),
                new RecordingProcessTaskMailService(),
                communicationService
        );
        var task = processInstanceTask(null);
        var logger = new RecordingProcessNodeExecutionLogger();

        assertThrows(ProcessNodeExecutionExceptionUnknown.class, () -> handler.handleResult(
                logger,
                null,
                new TestProcessNodeDefinition("Fallback task"),
                processNode("Nachricht"),
                processInstance(identity),
                task,
                null,
                new ProcessNodeExecutionResultTaskUpdated()
                        .setCommunicationRequest(new ProcessNodeExecutionResultCommunicationRequest(
                                identity.identityId(),
                                message,
                                null
                        ))
        ));

        assertEquals(ProcessTaskStatus.Failed, task.getStatus());
        assertNotNull(task.getFinished());
        assertEquals(1, savedTasks.size());
        assertEquals(0, logger.events.stream()
                .filter(event -> event.title().equals("Nachricht versendet"))
                .count());
    }

    @Test
    void handleResult_MarksTaskFailedWhenCommunicationIdentityIsMissing() {
        var communicationService = mock(CommunicationService.class);
        var savedTasks = new ArrayList<ProcessInstanceTaskEntity>();
        var handler = createHandler(
                savedTasks,
                Map.of(),
                new RecordingProcessTaskMailService(),
                communicationService
        );
        var task = processInstanceTask(null);
        var logger = new RecordingProcessNodeExecutionLogger();

        assertThrows(ProcessNodeExecutionExceptionMissingValue.class, () -> handler.handleResult(
                logger,
                null,
                new TestProcessNodeDefinition("Fallback task"),
                processNode("Nachricht"),
                processInstance(),
                task,
                null,
                new ProcessNodeExecutionResultTaskUpdated()
                        .setCommunicationRequest(new ProcessNodeExecutionResultCommunicationRequest(
                                "missing",
                                CommunicationMessage.of("Subject", "Body", "Body"),
                                null
                        ))
        ));

        verifyNoInteractions(communicationService);
        assertEquals(ProcessTaskStatus.Failed, task.getStatus());
        assertNotNull(task.getFinished());
        assertEquals(1, savedTasks.size());
        assertEquals(0, logger.events.stream()
                .filter(event -> event.title().equals("Nachricht versendet"))
                .count());
    }

    @Test
    void handleResult_AppliesOutputMappingsWithExplicitArrayIndices() throws ProcessNodeExecutionException {
        var savedTasks = new ArrayList<ProcessInstanceTaskEntity>();
        var handler = createHandler(savedTasks, Map.of(), new RecordingProcessTaskMailService());
        var task = processInstanceTask(null);

        handler.handleResult(
                new RecordingProcessNodeExecutionLogger(),
                null,
                new TestProcessNodeDefinition("Fallback task", List.of(
                        new ProcessNodeOutput("result", "Result", "Mapped result", "string")
                )),
                processNode("Pruefung", Map.of("result", "items.0.status")),
                processInstance(),
                task,
                null,
                new ProcessNodeExecutionResultTaskUpdated()
                        .setNodeData(Map.of("result", "done"))
                        .setProcessData(Map.of())
        );

        assertEquals(1, savedTasks.size());
        assertEquals(
                Map.of(
                        "items", List.of(
                                Map.of("status", "done")
                        )
                ),
                task.getProcessData()
        );
    }

    @Test
    void handleResult_AppliesOutputMappingsAcrossExistingWildcardBindings() throws ProcessNodeExecutionException {
        var savedTasks = new ArrayList<ProcessInstanceTaskEntity>();
        var handler = createHandler(savedTasks, Map.of(), new RecordingProcessTaskMailService());
        var task = processInstanceTask(null);

        handler.handleResult(
                new RecordingProcessNodeExecutionLogger(),
                null,
                new TestProcessNodeDefinition("Fallback task", List.of(
                        new ProcessNodeOutput("result", "Result", "Mapped result", "string")
                )),
                processNode("Pruefung", Map.of("result", "items.*.status")),
                processInstance(),
                task,
                null,
                new ProcessNodeExecutionResultTaskUpdated()
                        .setNodeData(Map.of("result", "done"))
                        .setProcessData(Map.of(
                                "items", List.of(
                                        Map.of("id", 1),
                                        Map.of("id", 2)
                                )
                        ))
        );

        assertEquals(1, savedTasks.size());
        assertEquals(
                Map.of(
                        "items", List.of(
                                Map.of("id", 1, "status", "done"),
                                Map.of("id", 2, "status", "done")
                        )
                ),
                task.getProcessData()
        );
    }

    @Test
    void handleResult_SendsAssignmentMail_ForNewAssignmentToOtherUser() throws ProcessNodeExecutionException {
        var triggeringUser = user("user-1", "Trigger User");
        var assignedUser = user("user-2", "Assigned User");
        var mailService = new RecordingProcessTaskMailService();
        var savedTasks = new ArrayList<ProcessInstanceTaskEntity>();
        var handler = createHandler(savedTasks, Map.of(
                triggeringUser.getId(), triggeringUser,
                assignedUser.getId(), assignedUser
        ), mailService);

        var logger = new RecordingProcessNodeExecutionLogger();
        var processInstance = processInstance();
        var processInstanceTask = processInstanceTask(null);
        var currentNode = processNode("Prüfung");
        var provider = new TestProcessNodeDefinition("Fallback task");

        handler.handleResult(
                logger,
                triggeringUser,
                provider,
                currentNode,
                processInstance,
                processInstanceTask,
                null,
                ProcessNodeExecutionResultTaskAssigned.of(assignedUser.getId())
        );

        assertEquals(1, savedTasks.size());
        assertEquals(assignedUser.getId(), processInstanceTask.getAssignedUserId());
        assertEquals(1, mailService.sendCount);
        assertEquals(assignedUser.getId(), mailService.lastAssignedUserId);
        assertFalse(mailService.lastReassignment);
        assertEquals(0, logger.exceptionCount);
    }

    @Test
    void handleResult_DoesNotSendAssignmentMail_ForSelfAssignment() throws ProcessNodeExecutionException {
        var triggeringUser = user("user-1", "Trigger User");
        var mailService = new RecordingProcessTaskMailService();
        var handler = createHandler(new ArrayList<>(), Map.of(triggeringUser.getId(), triggeringUser), mailService);

        handler.handleResult(
                new RecordingProcessNodeExecutionLogger(),
                triggeringUser,
                new TestProcessNodeDefinition("Fallback task"),
                processNode("Prüfung"),
                processInstance(),
                processInstanceTask(null),
                null,
                ProcessNodeExecutionResultTaskAssigned.of(triggeringUser.getId())
        );

        assertEquals(0, mailService.sendCount);
    }

    @Test
    void handleResult_DoesNotSendAssignmentMail_ForUnchangedAssignee() throws ProcessNodeExecutionException {
        var triggeringUser = user("user-1", "Trigger User");
        var assignedUser = user("user-2", "Assigned User");
        var mailService = new RecordingProcessTaskMailService();
        var handler = createHandler(new ArrayList<>(), Map.of(
                triggeringUser.getId(), triggeringUser,
                assignedUser.getId(), assignedUser
        ), mailService);

        handler.handleResult(
                new RecordingProcessNodeExecutionLogger(),
                triggeringUser,
                new TestProcessNodeDefinition("Fallback task"),
                processNode("Prüfung"),
                processInstance(),
                processInstanceTask(assignedUser.getId()),
                null,
                ProcessNodeExecutionResultTaskAssigned.of(assignedUser.getId())
        );

        assertEquals(0, mailService.sendCount);
    }

    @Test
    void handleResult_DoesNotFail_WhenAssignmentMailFails() {
        var triggeringUser = user("user-1", "Trigger User");
        var assignedUser = user("user-2", "Assigned User");
        var mailService = new RecordingProcessTaskMailService();
        mailService.throwOnSend = true;

        var savedTasks = new ArrayList<ProcessInstanceTaskEntity>();
        var handler = createHandler(savedTasks, Map.of(
                triggeringUser.getId(), triggeringUser,
                assignedUser.getId(), assignedUser
        ), mailService);
        var logger = new RecordingProcessNodeExecutionLogger();
        var task = processInstanceTask(null);

        assertDoesNotThrow(() -> handler.handleResult(
                logger,
                triggeringUser,
                new TestProcessNodeDefinition("Fallback task"),
                processNode("Prüfung"),
                processInstance(),
                task,
                null,
                ProcessNodeExecutionResultTaskAssigned.of(assignedUser.getId())
        ));

        assertEquals(1, savedTasks.size());
        assertEquals(assignedUser.getId(), task.getAssignedUserId());
        assertEquals(1, mailService.sendCount);
        assertEquals(1, logger.exceptionCount);
    }

    private static ProcessNodeExecutionResultHandler createHandler(List<ProcessInstanceTaskEntity> savedTasks,
                                                                   Map<String, UserEntity> users,
                                                                   RecordingProcessTaskMailService mailService) {
        return createHandler(savedTasks, users, mailService, null);
    }

    private static ProcessNodeExecutionResultHandler createHandler(List<ProcessInstanceTaskEntity> savedTasks,
                                                                   Map<String, UserEntity> users,
                                                                   RecordingProcessTaskMailService mailService,
                                                                   CommunicationService communicationService) {
        return new ProcessNodeExecutionResultHandler(
                null,
                communicationService,
                proxy(ProcessInstanceRepository.class),
                createTaskRepository(savedTasks),
                proxy(ProcessEdgeRepository.class),
                new TestUserService(users),
                mailService,
                null,
                null
        );
    }

    private static ProcessInstanceTaskRepository createTaskRepository(List<ProcessInstanceTaskEntity> savedTasks) {
        return ProcessNodeExecutionResultHandlerTest.<ProcessInstanceTaskRepository>proxy(ProcessInstanceTaskRepository.class, (methodName, args) -> switch (methodName) {
            case "save" -> {
                var entity = (ProcessInstanceTaskEntity) args[0];
                if (entity.getId() == null) {
                    entity.setId(100L + savedTasks.size());
                }
                savedTasks.add(entity);
                yield entity;
            }
            default -> null;
        });
    }

    private static ProcessInstanceEntity processInstance() {
        return processInstance(new IdentityData[0]);
    }

    private static ProcessInstanceEntity processInstance(IdentityData... identities) {
        var identityDataMap = new IdentityDataMap();
        for (var identity : identities) {
            identityDataMap.put(identity.identityId(), identity);
        }

        return new ProcessInstanceEntity(
                42L,
                "CASE-42",
                UUID.randomUUID().toString(),
                7,
                1,
                ProcessInstanceStatus.Running,
                null,
                null,
                List.of("AZ-123"),
                identityDataMap,
                Instant.now(),
                Instant.now(),
                null,
                null,
                Map.of(),
                11,
                null,
                null
        );
    }

    private static IdentityData identity(String identityId) {
        return new IdentityData(
                "session",
                identityId,
                IdentityType.Email,
                null,
                null,
                identityId + "@example.com",
                Map.of(),
                null,
                Map.of()
        );
    }

    private static IdentityData providerIdentity(String identityId) {
        return new IdentityData(
                "session",
                identityId,
                IdentityType.IdentityProvider,
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "bund-id",
                null,
                Map.of("name", "Sensitive Applicant"),
                23,
                Map.of("token", "sensitive")
        );
    }

    private static ProcessInstanceTaskEntity processInstanceTask(String assignedUserId) {
        return new ProcessInstanceTaskEntity(
                null,
                UUID.randomUUID().toString(),
                42L,
                7,
                1,
                11,
                null,
                null,
                null,
                ProcessTaskStatus.Running,
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                Map.of("existing", true),
                Map.of("meta", true),
                Map.of("data", true),
                Map.of(),
                assignedUserId,
                null,
                null,
                null,
                null
        );
    }

    private static ProcessNodeEntity processNode(String name) {
        return processNode(name, Map.of());
    }

    private static ProcessNodeEntity processNode(String name, Map<String, String> outputMappings) {
        return new ProcessNodeEntity()
                .setId(11)
                .setProcessId(7)
                .setProcessVersion(1)
                .setName(name)
                .setDataKey("taskNode")
                .setProcessNodeDefinitionKey("test/task")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(outputMappings);
    }

    private static UserEntity user(String id, String fullName) {
        return new UserEntity()
                .setId(id)
                .setEmail(id + "@example.com")
                .setFirstName(fullName)
                .setLastName(fullName)
                .setFullName(fullName)
                .setEnabled(true)
                .setVerified(true)
                .setDeletedInIdp(false);
    }

    private interface ProxyHandler {
        Object invoke(String methodName, Object[] args);
    }

    private static <T> T proxy(Class<T> type) {
        return proxy(type, (methodName, args) -> null);
    }

    private static <T> T proxy(Class<T> type, ProxyHandler handler) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + "Proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }

                    return handler.invoke(method.getName(), args);
                }
        ));
    }

    private static final class TestUserService extends UserService {
        private final Map<String, UserEntity> users;

        private TestUserService(Map<String, UserEntity> users) {
            super(null, null, null, null, null);
            this.users = users;
        }

        @Override
        public Optional<UserEntity> retrieve(String id) throws ResponseException {
            return Optional.ofNullable(users.get(id));
        }
    }

    private static final class RecordingProcessTaskMailService extends ProcessTaskMailService {
        private int sendCount;
        private boolean lastReassignment;
        private String lastAssignedUserId;
        private boolean throwOnSend;

        private RecordingProcessTaskMailService() {
            super(null, null, null);
        }

        @Override
        public void sendAssigned(UserEntity triggeringUser,
                                 UserEntity assignedUser,
                                 ProcessInstanceEntity processInstance,
                                 ProcessInstanceTaskEntity processInstanceTask,
                                 ProcessNodeEntity currentNode,
                                 ProcessNodeDefinition provider,
                                 boolean isReassignment) {
            sendCount++;
            lastAssignedUserId = assignedUser.getId();
            lastReassignment = isReassignment;

            if (throwOnSend) {
                throw new RuntimeException("mail send failed");
            }
        }
    }

    private static final class RecordingProcessNodeExecutionLogger extends ProcessNodeExecutionLogger {
        private int exceptionCount;
        private final List<RecordedLogEvent> events = new ArrayList<>();

        private RecordingProcessNodeExecutionLogger() {
            super(42L, 9L, null, null, null);
        }

        @Override
        public void logf(ProcessNodeExecutionLogLevel level,
                         Boolean isTechnical,
                         Boolean isAuditable,
                         String title,
                         String format,
                         Object... args) {
            events.add(new RecordedLogEvent(
                    level,
                    isTechnical,
                    isAuditable,
                    title,
                    String.format(format, args),
                    Map.of()
            ));
        }

        @Override
        public void logf(ProcessNodeExecutionLogLevel level,
                         Boolean isTechnical,
                         Boolean isAuditable,
                         String title,
                         Map<String, Object> details,
                         String format,
                         Object... args) {
            events.add(new RecordedLogEvent(
                    level,
                    isTechnical,
                    isAuditable,
                    title,
                    String.format(format, args),
                    details
            ));
        }

        @Override
        public void logException(ProcessNodeExecutionException exception) {
            exceptionCount++;
        }

        @Override
        public void logException(Exception exception) {
            exceptionCount++;
        }
    }

    private record RecordedLogEvent(
            ProcessNodeExecutionLogLevel level,
            boolean technical,
            boolean auditable,
            String title,
            String message,
            Map<String, Object> details
    ) {
    }

    private static final class TestProcessNodeDefinition implements ProcessNodeDefinition<AuthoredElementValues> {
        private final String name;
        private final List<ProcessNodeOutput> outputs;

        private TestProcessNodeDefinition(String name) {
            this(name, List.of());
        }

        private TestProcessNodeDefinition(String name, List<ProcessNodeOutput> outputs) {
            this.name = name;
            this.outputs = outputs;
        }

        @Override
        public String getParentPluginKey() {
            return "test";
        }

        @Override
        public String getComponentKey() {
            return "task";
        }

        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getAbstract() {
            return "Test provider";
        }

        @Override
        public String getDescription() {
            return "Test provider";
        }

        @Nonnull
        @Override
        public ProcessNodeType getType() {
            return ProcessNodeType.Action;
        }

        @Nonnull
        @Override
        public ProcessNodeExecutionType[] getExecutionTypes() {
            return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
        }

        @Nonnull
        @Override
        public List<ProcessNodePort> getPorts() {
            return List.of();
        }

        @Nonnull
        @Override
        public List<ProcessNodeOutput> getOutputs() {
            return outputs;
        }

        @Override
        public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Nonnull
        @Override
        public Class<AuthoredElementValues> getNodeConfigurationClass() {
            return AuthoredElementValues.class;
        }
    }
}
