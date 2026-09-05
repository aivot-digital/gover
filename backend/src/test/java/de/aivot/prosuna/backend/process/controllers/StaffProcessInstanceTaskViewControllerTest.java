package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.core.converters.JsonObjectConverter;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementStates;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.LinkButtonContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationLogger;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.process.controllers.StaffProcessInstanceTaskViewController;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.TaskViewEvent;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskUpdated;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentSetRepository;
import de.aivot.prosuna.backend.process.services.ProcessDataService;
import de.aivot.prosuna.backend.process.services.CaseNumberGeneratorService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceTaskService;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.ProcessVersionService;
import de.aivot.prosuna.backend.process.services.FileUploadMultipartInputService;
import de.aivot.prosuna.backend.process.workers.ProcessNodeExecutionResultHandler;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class StaffProcessInstanceTaskViewControllerTest {
    @Test
    void retrieve_AfterAutoSaveReloadPreservesClearedStaffTaskValue() throws ResponseException {
        var user = new UserEntity()
                .setId("user-1")
                .setFirstName("Ada")
                .setLastName("Lovelace");
        var now = Instant.now();

        var jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", user.getId())
        );

        var process = new ProcessEntity(
                7,
                "Test Process",
                11,
                UUID.randomUUID(),
                1,
                1,
                1,
                now,
                now
        );

        var version = new ProcessVersionEntity(
                process.getId(),
                1,
                ProcessVersionStatus.Published,
                "Test Process",
                null,
                now,
                now,
                now,
                null
        );

        var instance = new ProcessInstanceEntity(
                42L,
                null,
                UUID.randomUUID().toString(),
                process.getId(),
                version.getProcessVersion(),
                ProcessInstanceStatus.Running,
                null,
                null,
                List.of(),
                new IdentityDataMap(),
                now,
                now,
                null,
                null,
                Map.of(),
                11,
                null,
                null
        );

        var task = new ProcessInstanceTaskEntity(
                9L,
                UUID.randomUUID().toString(),
                instance.getId(),
                process.getId(),
                version.getProcessVersion(),
                11,
                null,
                null,
                null,
                ProcessTaskStatus.Running,
                null,
                now,
                now,
                null,
                null,
                Map.of("keep", "value"),
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null,
                null
        );

        var provider = new AutoSaveStaffProcessNodeDefinition();
        var node = new ProcessNodeEntity()
                .setId(11)
                .setProcessId(process.getId())
                .setProcessVersion(version.getProcessVersion())
                .setName("Staff node")
                .setDataKey("staffNode")
                .setProcessNodeDefinitionKey(provider.getKey())
                .setProcessNodeDefinitionVersion(provider.getMajorVersion())
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());

        var elementDerivationService = new TestElementDerivationService();
        var controller = new StaffProcessInstanceTaskViewController(
                new TestProcessInstanceService(instance),
                new TestProcessInstanceTaskService(task),
                new ProcessNodeDefinitionService(List.of(provider)),
                new TestProcessNodeService(node),
                new ApplyingProcessNodeExecutionResultHandler(),
                new TestUserService(user),
                new TestProcessNodeExecutionLoggerFactory(),
                elementDerivationService,
                new TestProcessService(process),
                new TestProcessVersionService(version),
                new PassthroughTaskViewMultipartInputService(),
                new TestProcessDataService()
        );

        controller.derive(
                jwt,
                instance.getId(),
                task.getId(),
                new AuthoredElementValues(),
                List.of(ElementDerivationOptions.ALL_ELEMENTS)
        );
        assertEquals(
                List.of(ElementDerivationOptions.ALL_ELEMENTS),
                elementDerivationService.lastRequest.derivationOptions().getSkipErrorsForElementIds()
        );

        var initialResponse = controller.retrieve(jwt, instance.getId(), task.getId());
        assertEquals("initial", initialResponse.data().get("defaultField"));

        var updatedResponse = controller.update(
                jwt,
                instance.getId(),
                task.getId(),
                "{\"defaultField\":null}",
                null,
                null,
                null,
                null
        );

        assertTrue(updatedResponse.data().containsKey("defaultField"));
        assertNull(updatedResponse.data().get("defaultField"));

        var savedDraft = (Map<?, ?>) task.getRuntimeData().get(ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY);
        assertTrue(savedDraft.containsKey("defaultField"));
        assertNull(savedDraft.get("defaultField"));

        var converter = new JsonObjectConverter(JsonMapperTestUtils.createMapper());
        task.setRuntimeData(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(task.getRuntimeData())));

        var reloadedResponse = controller.retrieve(jwt, instance.getId(), task.getId());
        assertTrue(reloadedResponse.data().containsKey("defaultField"));
        assertNull(reloadedResponse.data().get("defaultField"));
    }

    @Test
    void update_WithKnownEventAndInvalidInputsRejectsBeforeDispatch() {
        var provider = new EventValidatedStaffProcessNodeDefinition();
        var elementDerivationService = new TestElementDerivationService();
        elementDerivationService.result = new DerivedRuntimeElementData()
                .putError("requiredField", "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
        var fixture = createFixture(provider, elementDerivationService);

        var ex = assertThrows(
                ResponseException.class,
                () -> fixture.controller().update(
                        fixture.jwt(),
                        fixture.instance().getId(),
                        fixture.task().getId(),
                        "{\"requiredField\":\"\"}",
                        null,
                        null,
                        "complete",
                        null
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        var details = assertInstanceOf(DerivedRuntimeElementData.class, ex.getDetails());
        assertEquals(
                "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.",
                details.getElementStates().get("requiredField").getError()
        );
        assertEquals("staff-root", elementDerivationService.lastRequest.element().getId());
        assertEquals("", elementDerivationService.lastRequest.authoredElementValues().get("requiredField"));
        assertFalse(provider.eventInvoked);
        assertFalse(fixture.task().getRuntimeData().containsKey(ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY));
    }

    @Test
    void update_AutoSaveWithInvalidInputsPersistsDraftWithoutValidation() throws ResponseException {
        var provider = new EventValidatedStaffProcessNodeDefinition();
        var elementDerivationService = new TestElementDerivationService();
        elementDerivationService.result = new DerivedRuntimeElementData()
                .putError("requiredField", "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
        var fixture = createFixture(provider, elementDerivationService);

        var response = fixture.controller().update(
                fixture.jwt(),
                fixture.instance().getId(),
                fixture.task().getId(),
                "{\"requiredField\":\"\"}",
                null,
                null,
                null,
                null
        );

        assertEquals("", response.data().get("requiredField"));
        assertFalse(provider.eventInvoked);
        assertNull(elementDerivationService.lastRequest);
        var savedDraft = (Map<?, ?>) fixture.task().getRuntimeData().get(ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY);
        assertEquals("", savedDraft.get("requiredField"));
    }

    @Test
    void update_WithUnknownEventIsRejectedAndDoesNotAutoSave() {
        var provider = new EventValidatedStaffProcessNodeDefinition();
        var elementDerivationService = new TestElementDerivationService();
        var fixture = createFixture(provider, elementDerivationService);

        var ex = assertThrows(
                ResponseException.class,
                () -> fixture.controller().update(
                        fixture.jwt(),
                        fixture.instance().getId(),
                        fixture.task().getId(),
                        "{\"requiredField\":\"value\"}",
                        null,
                        null,
                        "unknown",
                        null
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Invalid event: unknown", ex.getMessage());
        assertFalse(provider.eventInvoked);
        assertNull(elementDerivationService.lastRequest);
        assertFalse(fixture.task().getRuntimeData().containsKey(ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY));
    }

    @Test
    void update_WithInlineStaffTaskEventIsAccepted() throws ResponseException {
        var provider = new InlineStaffTaskProcessNodeDefinition(null);
        var elementDerivationService = new TestElementDerivationService();
        var fixture = createFixture(provider, elementDerivationService);

        var response = fixture.controller().update(
                fixture.jwt(),
                fixture.instance().getId(),
                fixture.task().getId(),
                "{}",
                null,
                null,
                "inline-complete",
                null
        );

        assertEquals("inline-complete", provider.eventInvokedWith);
        assertEquals("inline-complete", fixture.task().getRuntimeData().get("event"));
        assertEquals("staff-root", assertInstanceOf(BaseElement.class, response.layout()).getId());
        assertEquals("staff-root", elementDerivationService.lastRequest.element().getId());
    }

    @Test
    void update_WithHrefLinkButtonStaffTaskEventIsRejected() {
        var provider = new InlineStaffTaskProcessNodeDefinition("https://example.org");
        var elementDerivationService = new TestElementDerivationService();
        var fixture = createFixture(provider, elementDerivationService);

        var ex = assertThrows(
                ResponseException.class,
                () -> fixture.controller().update(
                        fixture.jwt(),
                        fixture.instance().getId(),
                        fixture.task().getId(),
                        "{}",
                        null,
                        null,
                        "inline-complete",
                        null
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertNull(provider.eventInvokedWith);
        assertNull(elementDerivationService.lastRequest);
    }

    private static StaffTaskControllerFixture createFixture(ProcessNodeDefinition<AuthoredElementValues> provider,
                                                            TestElementDerivationService elementDerivationService) {
        var user = new UserEntity()
                .setId("user-1")
                .setFirstName("Ada")
                .setLastName("Lovelace");
        var now = Instant.now();

        var jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", user.getId())
        );

        var process = new ProcessEntity(
                7,
                "Test Process",
                11,
                UUID.randomUUID(),
                1,
                1,
                1,
                now,
                now
        );

        var version = new ProcessVersionEntity(
                process.getId(),
                1,
                ProcessVersionStatus.Published,
                "Test Process",
                null,
                now,
                now,
                now,
                null
        );

        var instance = new ProcessInstanceEntity(
                42L,
                null,
                UUID.randomUUID().toString(),
                process.getId(),
                version.getProcessVersion(),
                ProcessInstanceStatus.Running,
                null,
                null,
                List.of(),
                new IdentityDataMap(),
                now,
                now,
                null,
                null,
                Map.of(),
                11,
                null,
                null
        );

        var task = new ProcessInstanceTaskEntity(
                9L,
                UUID.randomUUID().toString(),
                instance.getId(),
                process.getId(),
                version.getProcessVersion(),
                11,
                null,
                null,
                null,
                ProcessTaskStatus.Running,
                null,
                now,
                now,
                null,
                null,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null,
                null
        );

        var node = new ProcessNodeEntity()
                .setId(11)
                .setProcessId(process.getId())
                .setProcessVersion(version.getProcessVersion())
                .setName("Staff node")
                .setDataKey("staffNode")
                .setProcessNodeDefinitionKey(provider.getKey())
                .setProcessNodeDefinitionVersion(provider.getMajorVersion())
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());

        var controller = new StaffProcessInstanceTaskViewController(
                new TestProcessInstanceService(instance),
                new TestProcessInstanceTaskService(task),
                new ProcessNodeDefinitionService(List.of(provider)),
                new TestProcessNodeService(node),
                new ApplyingProcessNodeExecutionResultHandler(),
                new TestUserService(user),
                new TestProcessNodeExecutionLoggerFactory(),
                elementDerivationService,
                new TestProcessService(process),
                new TestProcessVersionService(version),
                new PassthroughTaskViewMultipartInputService(),
                new TestProcessDataService()
        );

        return new StaffTaskControllerFixture(jwt, instance, task, controller);
    }

    private record StaffTaskControllerFixture(
            Jwt jwt,
            ProcessInstanceEntity instance,
            ProcessInstanceTaskEntity task,
            StaffProcessInstanceTaskViewController controller
    ) {
    }

    private static final class TestProcessInstanceService extends ProcessInstanceService {
        private final ProcessInstanceEntity instance;

        private TestProcessInstanceService(ProcessInstanceEntity instance) {
            super(null, null, mock(ProcessInstanceAttachmentSetRepository.class), null, null, mock(CaseNumberGeneratorService.class));
            this.instance = instance;
        }

        @Override
        public Optional<ProcessInstanceEntity> retrieve(@Nonnull Long id) {
            return Optional.of(instance);
        }
    }

    private static final class TestProcessInstanceTaskService extends ProcessInstanceTaskService {
        private final ProcessInstanceTaskEntity task;

        private TestProcessInstanceTaskService(ProcessInstanceTaskEntity task) {
            super(null);
            this.task = task;
        }

        @Override
        public Optional<ProcessInstanceTaskEntity> retrieve(@Nonnull Long id) {
            return Optional.of(task);
        }

        @Override
        public Optional<ProcessInstanceTaskEntity> retrieve(@Nonnull Specification<ProcessInstanceTaskEntity> specification) {
            return Optional.of(task);
        }
    }

    private static final class TestProcessNodeService extends ProcessNodeService {
        private final ProcessNodeEntity node;

        private TestProcessNodeService(ProcessNodeEntity node) {
            super(null, null, null, null, null, null, null, new ProsunaConfig());
            this.node = node;
        }

        @Override
        public Optional<ProcessNodeEntity> retrieve(@Nonnull Integer id) {
            return Optional.of(node);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <NodeConfig> ProcessConfigurationDetails<NodeConfig> deriveConfiguration(@Nonnull ProcessNodeEntity entity,
                                                                                        @Nonnull ProcessNodeDefinition<NodeConfig> provider,
                                                                                        UserEntity user,
                                                                                        @Nonnull Boolean skipErrors) {
            return new ProcessConfigurationDetails<>(
                    (NodeConfig) node.getConfiguration(),
                    new DerivedRuntimeElementData(new EffectiveElementValues(), new ComputedElementStates())
            );
        }
    }

    private static final class ApplyingProcessNodeExecutionResultHandler extends ProcessNodeExecutionResultHandler {
        private ApplyingProcessNodeExecutionResultHandler() {
            super(null, null, null, null, null, null, null, null, null);
        }

        @Override
        public void handleResult(ProcessNodeExecutionLogger logger,
                                 UserEntity triggeringUser,
                                 ProcessNodeDefinition provider,
                                 ProcessNodeEntity currentNode,
                                 ProcessInstanceEntity processInstance,
                                 ProcessInstanceTaskEntity processInstanceTask,
                                 ProcessInstanceTaskEntity previousTask,
                                 ProcessNodeExecutionResult executionResult) {
            var updatedTask = (ProcessNodeExecutionResultTaskUpdated) executionResult;
            processInstanceTask.setRuntimeData(updatedTask.getRuntimeData());
            processInstanceTask.setNodeData(updatedTask.getNodeData());
            processInstanceTask.setProcessData(updatedTask.getProcessData());
        }
    }

    private static final class TestUserService extends UserService {
        private final UserEntity user;

        private TestUserService(UserEntity user) {
            super(null, null, null, null, null);
            this.user = user;
        }

        @Override
        public Optional<UserEntity> fromJWT(Jwt jwt) {
            return Optional.of(user);
        }
    }

    private static final class TestProcessNodeExecutionLoggerFactory extends ProcessNodeExecutionLoggerFactory {
        private TestProcessNodeExecutionLoggerFactory() {
            super(null);
        }

        @Override
        public ProcessNodeExecutionLogger create(Long processInstanceId,
                                                 Long processInstanceTaskId,
                                                 String userId,
                                                 String identityId) {
            return new NoOpProcessNodeExecutionLogger(processInstanceId, processInstanceTaskId, userId, identityId);
        }
    }

    private static final class NoOpProcessNodeExecutionLogger extends ProcessNodeExecutionLogger {
        private final Long processInstanceId;
        private final String userId;
        private final String identityId;

        private NoOpProcessNodeExecutionLogger(Long processInstanceId,
                                               Long processInstanceTaskId,
                                               String userId,
                                               String identityId) {
            super(processInstanceId, processInstanceTaskId, userId, identityId, null);
            this.processInstanceId = processInstanceId;
            this.userId = userId;
            this.identityId = identityId;
        }

        @Override
        public ProcessNodeExecutionLogger withTaskId(Long taskId) {
            return new NoOpProcessNodeExecutionLogger(processInstanceId, taskId, userId, identityId);
        }

        @Override
        public void logf(ProcessNodeExecutionLogLevel level,
                         Boolean isTechnical,
                         Boolean isAuditable,
                         String title,
                         String format,
                         Object... args) {
        }

        @Override
        public void logException(ProcessNodeExecutionException exception) {
        }

        @Override
        public void logException(Exception exception) {
        }
    }

    private static final class TestElementDerivationService extends ElementDerivationService {
        private ElementDerivationRequest lastRequest;
        private DerivedRuntimeElementData result = new DerivedRuntimeElementData(new EffectiveElementValues(), new ComputedElementStates());

        private TestElementDerivationService() {
            super(null, null, null, null);
        }

        @Override
        public DerivedRuntimeElementData derive(ElementDerivationRequest request) {
            lastRequest = request;
            return result;
        }

        @Override
        public DerivedRuntimeElementData derive(ElementDerivationRequest request,
                                                IdentityDataMap identities,
                                                ElementDerivationLogger logger) {
            return new DerivedRuntimeElementData(new EffectiveElementValues(), new ComputedElementStates());
        }
    }

    private static final class TestProcessService extends ProcessService {
        private final ProcessEntity process;

        private TestProcessService(ProcessEntity process) {
            super(null, null, null);
            this.process = process;
        }

        @Override
        public Optional<ProcessEntity> retrieve(@Nonnull Integer id) {
            return Optional.of(process);
        }
    }

    private static final class TestProcessVersionService extends ProcessVersionService {
        private final ProcessVersionEntity version;

        private TestProcessVersionService(ProcessVersionEntity version) {
            super(null, null, null, mock(CaseNumberGeneratorService.class));
            this.version = version;
        }

        @Override
        public Optional<ProcessVersionEntity> retrieve(@Nonnull ProcessVersionEntityId id) {
            return Optional.of(version);
        }
    }

    private static final class PassthroughTaskViewMultipartInputService extends FileUploadMultipartInputService {
        private PassthroughTaskViewMultipartInputService() {
            super(null, null, null);
        }

        @Override
        public NormalizationResult normalizeInputs(BaseElement layout,
                                                   AuthoredElementValues inputs,
                                                   MultipartFile[] files,
                                                   List<String> fileUris,
                                                   Long processInstanceId,
                                                   Long processInstanceTaskId,
                                                   String uploadedByUserId) {
            return new NormalizationResult(inputs, List.of());
        }
    }

    private static final class TestProcessDataService extends ProcessDataService {
        private TestProcessDataService() {
            super(null, null, null, null);
        }

        @Override
        public ProcessExecutionData foldProcessInstanceData(@Nonnull ProcessInstanceEntity instance,
                                                            Integer previousNodeId,
                                                            @Nonnull ProcessInstanceTaskEntity currentTask) {
            return new ProcessExecutionData();
        }
    }

    private static final class EventValidatedStaffProcessNodeDefinition implements ProcessNodeDefinition<AuthoredElementValues> {
        private boolean eventInvoked;

        @Override
        public String getParentPluginKey() {
            return "test";
        }

        @Override
        public String getComponentKey() {
            return "staff-event-validation";
        }

        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Override
        public String getName() {
            return "Staff event validation";
        }

        @Override
        public String getAbstract() {
            return "Staff task event validation test provider";
        }

        @Override
        public String getDescription() {
            return "Staff task event validation test provider";
        }

        @Nonnull
        @Override
        public ProcessNodeType getType() {
            return ProcessNodeType.Action;
        }

        @Nonnull
        @Override
        public ProcessNodeExecutionType[] getExecutionTypes() {
            return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Manual};
        }

        @Nonnull
        @Override
        public List<ProcessNodePort> getPorts() {
            return List.of();
        }

        @Override
        public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Nonnull
        @Override
        public GroupLayoutElement getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<AuthoredElementValues> context) {
            var requiredField = new TextInputElement();
            requiredField.setId("requiredField");
            requiredField.setLabel("Required field");
            requiredField.setRequired(true);

            var layout = new GroupLayoutElement();
            layout.setId("staff-root");
            layout.setChildren(List.of(requiredField));
            return layout;
        }

        @Nonnull
        @Override
        public List<TaskViewEvent> getStaffTaskViewEvents(@Nonnull ProcessNodeExecutionContextUIStaff<AuthoredElementValues> context) {
            return List.of(new TaskViewEvent("Complete", "complete"));
        }

        @Nonnull
        @Override
        public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<AuthoredElementValues> context,
                                                                             @Nonnull AuthoredElementValues update,
                                                                             @Nonnull String event) {
            eventInvoked = true;
            return new ProcessNodeExecutionResultTaskUpdated()
                    .setRuntimeData(Map.of("event", event))
                    .setNodeData(Map.of())
                    .setProcessData(context.getThisTask().getProcessData())
                    .asOptional();
        }

        @Nonnull
        @Override
        public Class<AuthoredElementValues> getNodeConfigurationClass() {
            return AuthoredElementValues.class;
        }
    }

    private static final class InlineStaffTaskProcessNodeDefinition implements ProcessNodeDefinition<AuthoredElementValues> {
        private final String href;
        private String eventInvokedWith;

        private InlineStaffTaskProcessNodeDefinition(String href) {
            this.href = href;
        }

        @Override
        public String getParentPluginKey() {
            return "test";
        }

        @Override
        public String getComponentKey() {
            return "staff-inline-event";
        }

        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Override
        public String getName() {
            return "Staff inline event";
        }

        @Override
        public String getAbstract() {
            return "Staff inline task event test provider";
        }

        @Override
        public String getDescription() {
            return "Staff inline task event test provider";
        }

        @Nonnull
        @Override
        public ProcessNodeType getType() {
            return ProcessNodeType.Action;
        }

        @Nonnull
        @Override
        public ProcessNodeExecutionType[] getExecutionTypes() {
            return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Manual};
        }

        @Nonnull
        @Override
        public List<ProcessNodePort> getPorts() {
            return List.of();
        }

        @Override
        public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Nonnull
        @Override
        public GroupLayoutElement getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<AuthoredElementValues> context) {
            var linkButton = new LinkButtonContentElement()
                    .setLabel("Complete inline")
                    .setHref(href)
                    .setStaffTaskEvent("inline-complete");
            linkButton.setId("inline-button");

            var layout = new GroupLayoutElement();
            layout.setId("staff-root");
            layout.setChildren(List.of(linkButton));
            return layout;
        }

        @Nonnull
        @Override
        public List<TaskViewEvent> getStaffTaskViewEvents(@Nonnull ProcessNodeExecutionContextUIStaff<AuthoredElementValues> context) {
            return List.of();
        }

        @Nonnull
        @Override
        public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<AuthoredElementValues> context,
                                                                             @Nonnull AuthoredElementValues update,
                                                                             @Nonnull String event) {
            eventInvokedWith = event;
            return new ProcessNodeExecutionResultTaskUpdated()
                    .setRuntimeData(Map.of("event", event))
                    .setNodeData(Map.of())
                    .setProcessData(context.getThisTask().getProcessData())
                    .asOptional();
        }

        @Nonnull
        @Override
        public Class<AuthoredElementValues> getNodeConfigurationClass() {
            return AuthoredElementValues.class;
        }
    }

    private static final class AutoSaveStaffProcessNodeDefinition implements ProcessNodeDefinition<AuthoredElementValues> {
        @Override
        public String getParentPluginKey() {
            return "test";
        }

        @Override
        public String getComponentKey() {
            return "staff-autosave";
        }

        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Override
        public String getName() {
            return "Staff autosave";
        }

        @Override
        public String getAbstract() {
            return "Staff task test provider";
        }

        @Override
        public String getDescription() {
            return "Staff task test provider";
        }

        @Nonnull
        @Override
        public ProcessNodeType getType() {
            return ProcessNodeType.Action;
        }

        @Nonnull
        @Override
        public ProcessNodeExecutionType[] getExecutionTypes() {
            return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Manual};
        }

        @Nonnull
        @Override
        public List<ProcessNodePort> getPorts() {
            return List.of();
        }

        @Override
        public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Nonnull
        @Override
        public GroupLayoutElement getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<AuthoredElementValues> context) {
            var layout = new GroupLayoutElement();
            layout.setId("staff-root");
            return layout;
        }

        @Nonnull
        @Override
        public AuthoredElementValues createDefaultStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff<AuthoredElementValues> context) {
            var initialData = new AuthoredElementValues();
            initialData.put("defaultField", "initial");
            return initialData;
        }

        @Nonnull
        @Override
        public Class<AuthoredElementValues> getNodeConfigurationClass() {
            return AuthoredElementValues.class;
        }
    }
}
