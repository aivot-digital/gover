package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.core.converters.JsonObjectConverter;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.ComputedElementStates;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.identity.models.IdentityDataMap;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntityId;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskUpdated;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.process.services.CaseNumberGeneratorService;
import de.aivot.GoverBackend.process.services.ProcessInstanceService;
import de.aivot.GoverBackend.process.services.ProcessInstanceTaskService;
import de.aivot.GoverBackend.process.services.ProcessNodeDefinitionService;
import de.aivot.GoverBackend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.GoverBackend.process.services.ProcessNodeService;
import de.aivot.GoverBackend.process.services.ProcessService;
import de.aivot.GoverBackend.process.services.ProcessVersionService;
import de.aivot.GoverBackend.process.services.FileUploadMultipartInputService;
import de.aivot.GoverBackend.process.workers.ProcessNodeExecutionResultHandler;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
                UUID.randomUUID(),
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
                UUID.randomUUID(),
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

        var controller = new StaffProcessInstanceTaskViewController(
                new TestProcessInstanceService(instance),
                new TestProcessInstanceTaskService(task),
                new ProcessNodeDefinitionService(List.of(provider)),
                new TestProcessNodeService(node),
                new ApplyingProcessNodeExecutionResultHandler(),
                new TestUserService(user),
                new TestProcessNodeExecutionLoggerFactory(),
                new TestElementDerivationService(),
                new TestProcessService(process),
                new TestProcessVersionService(version),
                new PassthroughTaskViewMultipartInputService(),
                new TestProcessDataService()
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

        var converter = new JsonObjectConverter();
        task.setRuntimeData(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(task.getRuntimeData())));

        var reloadedResponse = controller.retrieve(jwt, instance.getId(), task.getId());
        assertTrue(reloadedResponse.data().containsKey("defaultField"));
        assertNull(reloadedResponse.data().get("defaultField"));
    }

    private static final class TestProcessInstanceService extends ProcessInstanceService {
        private final ProcessInstanceEntity instance;

        private TestProcessInstanceService(ProcessInstanceEntity instance) {
            super(null, null, null, null, mock(CaseNumberGeneratorService.class));
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
            super(null, null, null, null, null, null, null);
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

    private static final class TestUserService extends UserService {
        private final UserEntity user;

        private TestUserService(UserEntity user) {
            super(null, null, null, null);
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
        public void logf(de.aivot.GoverBackend.process.enums.ProcessNodeExecutionLogLevel level,
                         Boolean isTechnical,
                         Boolean isAuditable,
                         String title,
                         String format,
                         Object... args) {
        }

        @Override
        public void logException(de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException exception) {
        }

        @Override
        public void logException(Exception exception) {
        }
    }

    private static final class TestElementDerivationService extends ElementDerivationService {
        private TestElementDerivationService() {
            super(null, null, null);
        }

        @Override
        public DerivedRuntimeElementData derive(ElementDerivationRequest request) {
            return new DerivedRuntimeElementData(new EffectiveElementValues(), new ComputedElementStates());
        }

        @Override
        public DerivedRuntimeElementData derive(ElementDerivationRequest request,
                                                de.aivot.GoverBackend.identity.models.IdentityDataMap identities,
                                                de.aivot.GoverBackend.elements.services.ElementDerivationLogger logger) {
            return new DerivedRuntimeElementData(new EffectiveElementValues(), new ComputedElementStates());
        }
    }

    private static final class TestProcessService extends ProcessService {
        private final ProcessEntity process;

        private TestProcessService(ProcessEntity process) {
            super(null, null);
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
            super(null, null);
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
            super(null, null, null);
        }

        @Override
        public ProcessExecutionData foldProcessInstanceData(@Nonnull ProcessInstanceEntity instance,
                                                            Integer previousNodeId) {
            return new ProcessExecutionData();
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
