package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.elements.models.*;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.TaskViewEvent;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentSetRepository;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskUpdated;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.services.*;
import de.aivot.prosuna.backend.process.workers.ProcessNodeExecutionResultHandler;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class CitizenProcessInstanceTaskViewControllerTest {
    @Test
    void update_AutoSavePersistsNormalizedInputsAndReturnsMergedCustomerTaskViewData() throws ResponseException {
        var procAccess = UUID.randomUUID();
        var taskAccess = UUID.randomUUID();
        var now = Instant.now();

        var instance = new ProcessInstanceEntity(
                42L,
                null,
                procAccess,
                7,
                1,
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
                taskAccess,
                instance.getId(),
                instance.getProcessId(),
                1,
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
                Map.of("existing", "node-data"),
                Map.of("processField", "process-value"),
                Map.of(),
                null,
                null,
                null,
                null,
                null
        );

        var provider = new AutoSaveCustomerProcessNodeDefinition();
        var node = new ProcessNodeEntity()
                .setId(11)
                .setProcessId(instance.getProcessId())
                .setProcessVersion(1)
                .setName("Citizen node")
                .setDataKey("citizenNode")
                .setProcessNodeDefinitionKey(provider.getKey())
                .setProcessNodeDefinitionVersion(provider.getMajorVersion())
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());

        var normalizedInputs = new AuthoredElementValues();
        normalizedInputs.put("field", "normalized");
        normalizedInputs.put("extra", "saved");

        var controller = new CitizenProcessInstanceTaskViewController(
                new TestProcessInstanceService(instance),
                new TestProcessInstanceTaskService(task),
                new ProcessNodeDefinitionService(List.of(provider)),
                new TestProcessNodeService(node),
                new ApplyingProcessNodeExecutionResultHandler(),
                new TestProcessNodeExecutionLoggerFactory(),
                new TestElementDerivationService(normalizedInputs),
                new TestTaskViewMultipartInputService(normalizedInputs)
        );

        var response = controller.update(
                procAccess,
                taskAccess,
                "{\"field\":\"submitted\"}",
                null,
                null,
                null,
                null
        );

        assertEquals("initial", response.data().get("defaultField"));
        assertEquals("normalized", response.data().get("field"));
        assertEquals("saved", response.data().get("extra"));
        assertEquals(List.of(new TaskViewEvent("Submit", "submit")), response.events());
        assertEquals("value", task.getRuntimeData().get("keep"));
    }

    @Test
    void update_ReturnsNormalizedInputs_WhenCustomerUpdateIsNoOp() throws ResponseException {
        var procAccess = UUID.randomUUID();
        var taskAccess = UUID.randomUUID();

        var instance = new ProcessInstanceEntity(
                42L,
                null,
                procAccess,
                7,
                1,
                ProcessInstanceStatus.Running,
                null,
                null,
                List.of(),
                new IdentityDataMap(),
                Instant.now(),
                Instant.now(),
                null,
                null,
                Map.of(),
                11,
                null,
                null
        );

        var task = new ProcessInstanceTaskEntity(
                9L,
                taskAccess,
                instance.getId(),
                instance.getProcessId(),
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

        var provider = new NoOpCustomerProcessNodeDefinition();
        var node = new ProcessNodeEntity()
                .setId(11)
                .setProcessId(instance.getProcessId())
                .setProcessVersion(1)
                .setName("Citizen node")
                .setDataKey("citizenNode")
                .setProcessNodeDefinitionKey(provider.getKey())
                .setProcessNodeDefinitionVersion(provider.getMajorVersion())
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());

        var normalizedInputs = new AuthoredElementValues();
        normalizedInputs.put("field", "normalized");
        normalizedInputs.put("attachment", "process-instance-attachment:abc");

        var controller = new CitizenProcessInstanceTaskViewController(
                new TestProcessInstanceService(instance),
                new TestProcessInstanceTaskService(task),
                new ProcessNodeDefinitionService(List.of(provider)),
                new TestProcessNodeService(node),
                new FailingProcessNodeExecutionResultHandler(),
                new TestProcessNodeExecutionLoggerFactory(),
                new TestElementDerivationService(normalizedInputs),
                new TestTaskViewMultipartInputService(normalizedInputs)
        );

        var response = controller.update(
                procAccess,
                taskAccess,
                "{\"field\":\"submitted\"}",
                null,
                null,
                "submit",
                null
        );

        assertNotNull(response.data());
        assertEquals(normalizedInputs, response.data());
        assertEquals("customer-root", response.layout().getId());
        assertEquals(List.of(new TaskViewEvent("Submit", "submit")), response.events());
    }

    private static final class TestProcessInstanceService extends ProcessInstanceService {
        private final ProcessInstanceEntity instance;

        private TestProcessInstanceService(ProcessInstanceEntity instance) {
            super(null, null, mock(ProcessInstanceAttachmentSetRepository.class), null, null, mock(CaseNumberGeneratorService.class));
            this.instance = instance;
        }

        @Override
        public Optional<ProcessInstanceEntity> retrieve(Specification<ProcessInstanceEntity> specification) {
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
        public Optional<ProcessInstanceTaskEntity> retrieve(Specification<ProcessInstanceTaskEntity> specification) {
            return Optional.of(task);
        }
    }

    private static final class TestProcessNodeService extends ProcessNodeService {
        private final ProcessNodeEntity node;

        private TestProcessNodeService(ProcessNodeEntity node) {
            super(null, null, null, null, null, null, null, new ProsunaConfig());
            this.node = node;
        }

        @Nonnull
        @Override
        public Optional<ProcessNodeEntity> retrieve(@Nonnull Integer id) {
            return Optional.of(node);
        }
    }

    private static final class TestTaskViewMultipartInputService extends FileUploadMultipartInputService {
        private final AuthoredElementValues normalizedInputs;

        private TestTaskViewMultipartInputService(AuthoredElementValues normalizedInputs) {
            super(null, null, null);
            this.normalizedInputs = normalizedInputs;
        }

        @Override
        public NormalizationResult normalizeInputs(BaseElement layout,
                                                   AuthoredElementValues inputs,
                                                   MultipartFile[] files,
                                                   List<String> fileUris,
                                                   Long processInstanceId,
                                                   Long processInstanceTaskId,
                                                   String uploadedByUserId) {
            return new NormalizationResult(normalizedInputs, List.of());
        }
    }

    private static final class TestElementDerivationService extends ElementDerivationService {
        private final AuthoredElementValues normalizedInputs;

        private TestElementDerivationService(AuthoredElementValues normalizedInputs) {
            super(null, null, null, null);
            this.normalizedInputs = normalizedInputs;
        }

        @Override
        public DerivedRuntimeElementData derive(ElementDerivationRequest request) {
            var effectiveValues = new EffectiveElementValues();
            effectiveValues.putAll(normalizedInputs);
            return new DerivedRuntimeElementData(effectiveValues, new ComputedElementStates());
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

    private static final class FailingProcessNodeExecutionResultHandler extends ProcessNodeExecutionResultHandler {
        private FailingProcessNodeExecutionResultHandler() {
            super(null, null, null, null, null, null, null, null);
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
            throw new AssertionError("handleResult should not be called for no-op customer updates");
        }
    }

    private static final class ApplyingProcessNodeExecutionResultHandler extends ProcessNodeExecutionResultHandler {
        private ApplyingProcessNodeExecutionResultHandler() {
            super(null, null, null, null, null, null, null, null);
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

    private static final class NoOpCustomerProcessNodeDefinition implements ProcessNodeDefinition<AuthoredElementValues> {
        @Override
        public String getParentPluginKey() {
            return "test";
        }

        @Override
        public String getComponentKey() {
            return "citizen-noop";
        }

        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Override
        public String getName() {
            return "Citizen no-op";
        }

        @Override
        public String getAbstract() {
            return "Customer task test provider";
        }

        @Override
        public String getDescription() {
            return "Customer task test provider";
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
        public GroupLayoutElement getCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer context) {
            var layout = new GroupLayoutElement();
            layout.setId("customer-root");
            return layout;
        }

        @Nonnull
        @Override
        public List<TaskViewEvent> getCustomerTaskViewEvents(@Nonnull ProcessNodeExecutionContextUICustomer context) {
            return List.of(new TaskViewEvent("Submit", "submit"));
        }

        @Nonnull
        @Override
        public AuthoredElementValues getCustomerTaskViewData(@Nonnull ProcessNodeExecutionContextUICustomer context) {
            var persistedData = new AuthoredElementValues();
            persistedData.put("field", "persisted");
            return persistedData;
        }

        @Nonnull
        @Override
        public Class<AuthoredElementValues> getNodeConfigurationClass() {
            return AuthoredElementValues.class;
        }
    }

    private static final class AutoSaveCustomerProcessNodeDefinition implements ProcessNodeDefinition<AuthoredElementValues> {
        @Override
        public String getParentPluginKey() {
            return "test";
        }

        @Override
        public String getComponentKey() {
            return "citizen-autosave";
        }

        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Override
        public String getName() {
            return "Citizen autosave";
        }

        @Override
        public String getAbstract() {
            return "Customer autosave test provider";
        }

        @Override
        public String getDescription() {
            return "Customer autosave test provider";
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
        public GroupLayoutElement getCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer context) {
            var layout = new GroupLayoutElement();
            layout.setId("customer-root");
            return layout;
        }

        @Nonnull
        @Override
        public List<TaskViewEvent> getCustomerTaskViewEvents(@Nonnull ProcessNodeExecutionContextUICustomer context) {
            return List.of(new TaskViewEvent("Submit", "submit"));
        }

        @Nonnull
        @Override
        public AuthoredElementValues createDefaultCustomerTaskViewData(@Nonnull ProcessNodeExecutionContextUICustomer context) {
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
