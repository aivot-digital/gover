package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessWorkerTest {
    @ParameterizedTest
    @EnumSource(value = ProcessInstanceStatus.class, names = {"Completed", "Aborted"})
    void queuedWork_DoesNotExecuteForTerminalInstance(ProcessInstanceStatus status) {
        var instance = mock(ProcessInstanceEntity.class);
        when(instance.getStatus()).thenReturn(status);
        var instanceRepository = mock(ProcessInstanceRepository.class);
        when(instanceRepository.findById(42L)).thenReturn(Optional.of(instance));
        var nodeRepository = mock(ProcessNodeRepository.class);
        var taskRepository = mock(ProcessInstanceTaskRepository.class);
        var definitionService = mock(ProcessNodeDefinitionService.class);
        var resultHandler = mock(ProcessNodeExecutionResultHandler.class);
        var worker = new ProcessWorker(
                instanceRepository,
                nodeRepository,
                definitionService,
                taskRepository,
                resultHandler,
                mock(ProcessDataService.class),
                mock(ProcessNodeExecutionLoggerFactory.class),
                mock(ProcessNodeService.class)
        );

        worker.doWorkOnNextNode(new ProcessWorker.DoWorkWorkerPayload(42L, null, null, null, 11));
        worker.resumeWorkOnCurrentNode(new ProcessWorker.ResumeWorkWorkerPayload(42L, 100L, 11));

        verifyNoInteractions(nodeRepository, taskRepository, definitionService, resultHandler);
        verify(instanceRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(ExecutionFailure.class)
    void doWork_MarksProcessInstanceAndTaskFailed_WhenExecutionFails(ExecutionFailure executionFailure) {
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
        var savedEvents = new ArrayList<ProcessInstanceEventEntity>();

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
        var processInstanceHistoryEventRepository = createProxy(ProcessInstanceHistoryEventRepository.class, (methodName, args) -> switch (methodName) {
            case "save" -> {
                var event = (ProcessInstanceEventEntity) args[0];
                savedEvents.add(event);
                yield event;
            }
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
                        processInstanceAttachmentSetRepository,
                        JsonMapperTestUtils.createMapper()
                ),
                new ProcessNodeExecutionLoggerFactory(processInstanceHistoryEventRepository),
                new TestProcessNodeService(executionFailure == ExecutionFailure.RUNTIME_CONFIGURATION)
        );

        worker.doWorkOnNextNode(new ProcessWorker.DoWorkWorkerPayload(42L, null, null, null, 11));

        assertEquals(ProcessInstanceStatus.Failed, processInstance.getStatus());
        assertEquals(1, savedProcessInstances.size());
        assertEquals(processInstance, savedProcessInstances.getFirst());

        assertEquals(2, savedTasks.size());
        var failedTask = savedTasks.getLast();
        assertEquals(ProcessTaskStatus.Failed, failedTask.getStatus());
        assertNotNull(failedTask.getFinished());
        assertEquals(executionFailure == ExecutionFailure.PROVIDER, processNodeDefinition.wasInitCalled());
        assertFalse(resultHandler.wasHandleResultCalled());

        var errorEvents = savedEvents.stream()
                .filter(event -> event.getLevel() == ProcessNodeExecutionLogLevel.Error)
                .toList();
        assertEquals(1, errorEvents.size());
        assertEquals(failedTask.getId(), errorEvents.getFirst().getProcessInstanceTaskId());
    }

    @ParameterizedTest
    @EnumSource(ExecutionFailure.class)
    void resumeWork_MarksProcessInstanceAndTaskFailed_WhenExecutionFails(ExecutionFailure executionFailure) {
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
                null,
                null
        );

        var savedProcessInstances = new ArrayList<ProcessInstanceEntity>();
        var savedTasks = new ArrayList<ProcessInstanceTaskEntity>();
        var savedEvents = new ArrayList<ProcessInstanceEventEntity>();

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
        var processInstanceHistoryEventRepository = createProxy(ProcessInstanceHistoryEventRepository.class, (methodName, args) -> switch (methodName) {
            case "save" -> {
                var event = (ProcessInstanceEventEntity) args[0];
                savedEvents.add(event);
                yield event;
            }
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
                        processInstanceAttachmentSetRepository,
                        JsonMapperTestUtils.createMapper()
                ),
                new ProcessNodeExecutionLoggerFactory(processInstanceHistoryEventRepository),
                new TestProcessNodeService(executionFailure == ExecutionFailure.RUNTIME_CONFIGURATION)
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
        assertEquals(executionFailure == ExecutionFailure.PROVIDER, processNodeDefinition.wasResumeCalled());
        assertFalse(resultHandler.wasHandleResultCalled());

        var errorEvents = savedEvents.stream()
                .filter(event -> event.getLevel() == ProcessNodeExecutionLogLevel.Error)
                .toList();
        assertEquals(1, errorEvents.size());
        assertEquals(failedTask.getId(), errorEvents.getFirst().getProcessInstanceTaskId());
    }

    private enum ExecutionFailure {
        RUNTIME_CONFIGURATION,
        PROVIDER,
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
        private boolean initCalled;
        private boolean resumeCalled;

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
            initCalled = true;
            throw new RuntimeException("init failure");
        }

        @Override
        public ProcessNodeExecutionResult resume(@Nonnull ProcessNodeExecutionInitContext<AuthoredElementValues> context) {
            resumeCalled = true;
            throw new RuntimeException("resume failure");
        }

        private boolean wasInitCalled() {
            return initCalled;
        }

        private boolean wasResumeCalled() {
            return resumeCalled;
        }

        @Nonnull
        @Override
        public Class<AuthoredElementValues> getNodeConfigurationClass() {
            return AuthoredElementValues.class;
        }
    }

    private static final class TestProcessNodeService extends ProcessNodeService {
        private final boolean invalidRuntimeConfiguration;

        private TestProcessNodeService(boolean invalidRuntimeConfiguration) {
            super(null, null, null, null, null, null, null, new ProsunaConfig(), null, null);
            this.invalidRuntimeConfiguration = invalidRuntimeConfiguration;
        }

        @Nonnull
        @Override
        public <NodeConfig> ProcessConfigurationDetails<NodeConfig> deriveRuntimeConfiguration(
                @Nonnull ProcessNodeEntity entity,
                @Nonnull ProcessNodeDefinition<NodeConfig> provider,
                UserEntity user,
                @Nonnull Boolean skipErrors,
                @Nonnull ProcessExecutionData processExecutionData
        ) {
            @SuppressWarnings("unchecked")
            var configuration = (NodeConfig) new AuthoredElementValues();
            var derivedRuntimeElementData = new DerivedRuntimeElementData();
            if (invalidRuntimeConfiguration) {
                derivedRuntimeElementData.putError("configuration", "invalid runtime configuration");
            }
            return new ProcessConfigurationDetails<>(configuration, derivedRuntimeElementData);
        }
    }

    private static final class TestProcessNodeExecutionResultHandler extends ProcessNodeExecutionResultHandler {
        private boolean handleResultCalled;

        private TestProcessNodeExecutionResultHandler() {
            super(null, null, null, null, null, null, null, null, null, null, null);
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
