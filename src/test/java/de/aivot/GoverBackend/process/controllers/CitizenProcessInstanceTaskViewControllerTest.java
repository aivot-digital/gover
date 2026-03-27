package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.ComputedElementStates;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextInit;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextUICustomer;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.models.TaskViewEvent;
import de.aivot.GoverBackend.process.services.ProcessInstanceService;
import de.aivot.GoverBackend.process.services.ProcessInstanceTaskService;
import de.aivot.GoverBackend.process.services.ProcessNodeDefinitionService;
import de.aivot.GoverBackend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.GoverBackend.process.services.ProcessNodeService;
import de.aivot.GoverBackend.process.services.TaskViewMultipartInputService;
import de.aivot.GoverBackend.process.workers.ProcessNodeExecutionResultHandler;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CitizenProcessInstanceTaskViewControllerTest {
    @Test
    void update_ReturnsNormalizedInputs_WhenCustomerUpdateIsNoOp() throws ResponseException {
        var procAccess = UUID.randomUUID();
        var taskAccess = UUID.randomUUID();

        var instance = new ProcessInstanceEntity(
                42L,
                procAccess,
                7,
                1,
                ProcessInstanceStatus.Running,
                null,
                null,
                List.of(),
                Map.of(),
                LocalDateTime.now(),
                LocalDateTime.now(),
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
                ProcessTaskStatus.Running,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
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
            super(null, null, null);
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
            super(null, null, null, null, null, null);
            this.node = node;
        }

        @Override
        public Optional<ProcessNodeEntity> retrieve(Integer id) {
            return Optional.of(node);
        }
    }

    private static final class TestTaskViewMultipartInputService extends TaskViewMultipartInputService {
        private final AuthoredElementValues normalizedInputs;

        private TestTaskViewMultipartInputService(AuthoredElementValues normalizedInputs) {
            super(null, null);
            this.normalizedInputs = normalizedInputs;
        }

        @Override
        public AuthoredElementValues normalizeInputs(AuthoredElementValues inputs,
                                                     MultipartFile[] files,
                                                     List<String> fileUris,
                                                     Long processInstanceId,
                                                     Long processInstanceTaskId,
                                                     String uploadedByUserId) {
            return normalizedInputs;
        }
    }

    private static final class TestElementDerivationService extends ElementDerivationService {
        private final AuthoredElementValues normalizedInputs;

        private TestElementDerivationService(AuthoredElementValues normalizedInputs) {
            super(null, null, null);
            this.normalizedInputs = normalizedInputs;
        }

        @Override
        public DerivedRuntimeElementData derive(ElementDerivationRequest request, ElementDerivationLogger logger) {
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
        public void logf(de.aivot.GoverBackend.process.enums.ProcessNodeExecutionLogLevel level,
                         Boolean isTechnical,
                         Boolean isAuditable,
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

    private static final class FailingProcessNodeExecutionResultHandler extends ProcessNodeExecutionResultHandler {
        private FailingProcessNodeExecutionResultHandler() {
            super(null, null, null, null, null, null);
        }

        @Override
        public void handleResult(ProcessNodeExecutionLogger logger,
                                 de.aivot.GoverBackend.user.entities.UserEntity triggeringUser,
                                 ProcessNodeDefinition provider,
                                 ProcessNodeEntity currentNode,
                                 ProcessInstanceEntity processInstance,
                                 ProcessInstanceTaskEntity processInstanceTask,
                                 ProcessInstanceTaskEntity previousTask,
                                 ProcessNodeExecutionResult executionResult) {
            throw new AssertionError("handleResult should not be called for no-op customer updates");
        }
    }

    private static final class NoOpCustomerProcessNodeDefinition implements ProcessNodeDefinition {
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
        public String getDescription() {
            return "Customer task test provider";
        }

        @Override
        public ProcessNodeType getType() {
            return ProcessNodeType.Action;
        }

        @Override
        public List<ProcessNodePort> getPorts() {
            return List.of();
        }

        @Override
        public ProcessNodeExecutionResult init(ProcessNodeExecutionContextInit context) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Override
        public GroupLayoutElement getCustomerTaskView(ProcessNodeExecutionContextUICustomer context) {
            var layout = new GroupLayoutElement();
            layout.setId("customer-root");
            return layout;
        }

        @Override
        public List<TaskViewEvent> getCustomerTaskViewEvents(ProcessNodeExecutionContextUICustomer context) {
            return List.of(new TaskViewEvent("Submit", "submit"));
        }

        @Override
        public AuthoredElementValues getCustomerTaskViewData(ProcessNodeExecutionContextUICustomer context) {
            var persistedData = new AuthoredElementValues();
            persistedData.put("field", "persisted");
            return persistedData;
        }
    }
}
