package de.aivot.GoverBackend.process.workers;

import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextInit;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.process.services.ProcessNodeDefinitionService;
import de.aivot.GoverBackend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.GoverBackend.process.services.ProcessNodeService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProcessWorkerTest {
    @Test
    void listen_MarksProcessInstanceFailed_WhenInitThrowsRuntimeException() {
        var processInstance = new ProcessInstanceEntity(
                42L,
                UUID.randomUUID(),
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

        var processNodeDefinition = new ThrowingProcessNodeDefinition();
        var processNode = new ProcessNodeEntity()
                .setId(11)
                .setProcessId(7)
                .setProcessVersion(1)
                .setName("Init node")
                .setDataKey("initNode")
                .setProcessNodeDefinitionKey(processNodeDefinition.getKey())
                .setProcessNodeDefinitionVersion(processNodeDefinition.getMajorVersion())
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());

        var savedProcessInstances = new ArrayList<ProcessInstanceEntity>();
        var savedTasks = new ArrayList<ProcessInstanceTaskEntity>();

        var processInstanceRepository = createProxy(ProcessInstanceRepository.class, (methodName, args) -> switch (methodName) {
            case "findById" -> Optional.of(processInstance);
            case "save" -> {
                var entity = (ProcessInstanceEntity) args[0];
                savedProcessInstances.add(entity);
                yield entity;
            }
            default -> defaultValue(args);
        });

        var processNodeRepository = createProxy(ProcessNodeRepository.class, (methodName, args) -> switch (methodName) {
            case "findById" -> Optional.of(processNode);
            case "findAllByProcessId" -> List.of(processNode);
            default -> defaultValue(args);
        });

        var processInstanceTaskRepository = createProxy(ProcessInstanceTaskRepository.class, (methodName, args) -> switch (methodName) {
            case "save" -> {
                var task = (ProcessInstanceTaskEntity) args[0];
                if (task.getId() == null) {
                    task.setId(100L + savedTasks.size());
                }
                savedTasks.add(task);
                yield task;
            }
            case "getLatestTasksByProcessInstanceId" -> List.of();
            case "findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc" -> null;
            default -> defaultValue(args);
        });

        var processInstanceAttachmentRepository = createProxy(ProcessInstanceAttachmentRepository.class, (methodName, args) -> switch (methodName) {
            case "findAllByProcessInstanceId" -> List.of();
            default -> defaultValue(args);
        });

        var resultHandler = new TestProcessNodeExecutionResultHandler();

        var worker = new ProcessWorker(
                processInstanceRepository,
                processNodeRepository,
                new ProcessNodeDefinitionService(List.of(processNodeDefinition)),
                processInstanceTaskRepository,
                resultHandler,
                new ProcessDataService(
                        processInstanceTaskRepository,
                        processNodeRepository,
                        processInstanceAttachmentRepository
                ),
                new TestProcessNodeExecutionLoggerFactory(),
                new TestProcessNodeService()
        );

        worker.listen(new ProcessWorker.WorkerPayload(42L, null, 11));

        assertEquals(ProcessInstanceStatus.Failed, processInstance.getStatus());
        assertEquals(1, savedProcessInstances.size());
        assertEquals(processInstance, savedProcessInstances.getFirst());

        assertEquals(2, savedTasks.size());
        var failedTask = savedTasks.getLast();
        assertEquals(ProcessTaskStatus.Failed, failedTask.getStatus());
        assertNotNull(failedTask.getFinished());
        assertFalse(resultHandler.wasHandleResultCalled());
    }

    private interface ProxyHandler {
        Object invoke(String methodName, Object[] args);
    }

    private static <T> T createProxy(Class<T> type, ProxyHandler handler) {
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

    private static Object defaultValue(Object[] args) {
        return null;
    }

    private static final class ThrowingProcessNodeDefinition implements ProcessNodeDefinition {
        @Override
        public String getParentPluginKey() {
            return "test";
        }

        @Override
        public String getComponentKey() {
            return "throwing-init";
        }

        @Override
        public String getComponentVersion() {
            return "1.0.0";
        }

        @Override
        public String getName() {
            return "Throwing init";
        }

        @Override
        public String getDescription() {
            return "Test node";
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
            throw new RuntimeException("init failure");
        }
    }

    private static final class TestProcessNodeService extends ProcessNodeService {
        private TestProcessNodeService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public DerivedRuntimeElementData deriveConfiguration(ProcessNodeEntity entity, boolean skipErrors) {
            return new DerivedRuntimeElementData();
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
            return new TestProcessNodeExecutionLogger(processInstanceId, processInstanceTaskId, userId, identityId);
        }
    }

    private static final class TestProcessNodeExecutionLogger extends ProcessNodeExecutionLogger {
        private final Long processInstanceId;
        private final String userId;
        private final String identityId;

        private TestProcessNodeExecutionLogger(Long processInstanceId,
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
            return new TestProcessNodeExecutionLogger(processInstanceId, taskId, userId, identityId);
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

    private static final class TestProcessNodeExecutionResultHandler extends ProcessNodeExecutionResultHandler {
        private boolean handleResultCalled;

        private TestProcessNodeExecutionResultHandler() {
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
            handleResultCalled = true;
        }

        private boolean wasHandleResultCalled() {
            return handleResultCalled;
        }
    }
}
