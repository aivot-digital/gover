package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
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
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.ProcessDataService;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProcessWorkerTest {
    @Test
    void doWork_MarksProcessInstanceFailed_WhenInitThrowsRuntimeException() {
        var processInstance = new ProcessInstanceEntity(
                42L,
                null,
                UUID.randomUUID().toString(),
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
        var processInstanceAttachmentSetRepository = createProxy(ProcessInstanceAttachmentSetRepository.class, (methodName, args) -> switch (methodName) {
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
                        processInstanceAttachmentRepository,
                        processInstanceAttachmentSetRepository
                ),
                new TestProcessNodeExecutionLoggerFactory(),
                new TestProcessNodeService()
        );

        worker.doWorkOnNextNode(new ProcessWorker.DoWorkWorkerPayload(42L, null, null, null, 11));

        assertEquals(ProcessInstanceStatus.Failed, processInstance.getStatus());
        assertEquals(1, savedProcessInstances.size());
        assertEquals(processInstance, savedProcessInstances.getFirst());

        assertEquals(2, savedTasks.size());
        var failedTask = savedTasks.getLast();
        assertEquals(ProcessTaskStatus.Failed, failedTask.getStatus());
        assertNotNull(failedTask.getFinished());
        assertFalse(resultHandler.wasHandleResultCalled());
    }

    @Test
    void resumeWork_MarksProcessInstanceFailed_WhenResumeThrowsRuntimeException() {
        var processInstance = new ProcessInstanceEntity(
                42L,
                null,
                UUID.randomUUID().toString(),
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

        var processNodeDefinition = new ThrowingProcessNodeDefinition();
        var processNode = new ProcessNodeEntity()
                .setId(11)
                .setProcessId(7)
                .setProcessVersion(1)
                .setName("Resume node")
                .setDataKey("resumeNode")
                .setProcessNodeDefinitionKey(processNodeDefinition.getKey())
                .setProcessNodeDefinitionVersion(processNodeDefinition.getMajorVersion())
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());

        var currentTask = new ProcessInstanceTaskEntity(
                100L,
                UUID.randomUUID().toString(),
                processInstance.getId(),
                processInstance.getProcessId(),
                1,
                processNode.getId(),
                null,
                null,
                null,
                ProcessTaskStatus.Running,
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                null,
                null,
                null,
                null,
                null
        );

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
            case "findById" -> Optional.of(currentTask);
            case "save" -> {
                var task = (ProcessInstanceTaskEntity) args[0];
                savedTasks.add(task);
                yield task;
            }
            case "getLatestTasksByProcessInstanceId" -> List.of(currentTask);
            case "findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc" -> Optional.empty();
            default -> defaultValue(args);
        });

        var processInstanceAttachmentRepository = createProxy(ProcessInstanceAttachmentRepository.class, (methodName, args) -> switch (methodName) {
            case "findAllByProcessInstanceId" -> List.of();
            default -> defaultValue(args);
        });
        var processInstanceAttachmentSetRepository = createProxy(ProcessInstanceAttachmentSetRepository.class, (methodName, args) -> switch (methodName) {
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
                        processInstanceAttachmentRepository,
                        processInstanceAttachmentSetRepository
                ),
                new TestProcessNodeExecutionLoggerFactory(),
                new TestProcessNodeService()
        );

        worker.resumeWorkOnCurrentNode(new ProcessWorker.ResumeWorkWorkerPayload(42L, 100L, 11));

        assertEquals(ProcessInstanceStatus.Failed, processInstance.getStatus());
        assertEquals(1, savedProcessInstances.size());
        assertEquals(processInstance, savedProcessInstances.getFirst());

        assertEquals(1, savedTasks.size());
        var failedTask = savedTasks.getFirst();
        assertEquals(currentTask, failedTask);
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

    private static final class ThrowingProcessNodeDefinition implements ProcessNodeDefinition<AuthoredElementValues> {
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
        public String getAbstract() {
            return "Test node";
        }

        @Override
        public String getDescription() {
            return "Test node";
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

        @Override
        public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) {
            throw new RuntimeException("init failure");
        }

        @Override
        public ProcessNodeExecutionResult resume(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) {
            throw new RuntimeException("resume failure");
        }

        @Nonnull
        @Override
        public Class<AuthoredElementValues> getNodeConfigurationClass() {
            return AuthoredElementValues.class;
        }
    }

    private static final class TestProcessNodeService extends ProcessNodeService {
        private TestProcessNodeService() {
            super(null, null, null, null, null, null, null, new ProsunaConfig());
        }

        @Nonnull
        @Override
        public <NodeConfig> ProcessConfigurationDetails<NodeConfig> deriveConfiguration(@Nonnull ProcessNodeEntity entity,
                                                                                        @Nonnull ProcessNodeDefinition<NodeConfig> provider,
                                                                                        UserEntity user,
                                                                                        @Nonnull Boolean skipErrors) {
            @SuppressWarnings("unchecked")
            var configuration = (NodeConfig) new AuthoredElementValues();
            return new ProcessConfigurationDetails<>(configuration, new DerivedRuntimeElementData());
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

    private static final class TestProcessNodeExecutionResultHandler extends ProcessNodeExecutionResultHandler {
        private boolean handleResultCalled;

        private TestProcessNodeExecutionResultHandler() {
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
            handleResultCalled = true;
        }

        private boolean wasHandleResultCalled() {
            return handleResultCalled;
        }
    }
}
