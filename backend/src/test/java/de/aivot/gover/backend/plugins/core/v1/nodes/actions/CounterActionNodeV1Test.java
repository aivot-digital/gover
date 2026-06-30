package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import de.aivot.gover.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.gover.backend.elements.models.*;
import de.aivot.gover.backend.elements.utils.ElementPOJOMapper;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.plugins.core.v1.nodes.actions.CounterActionNodeV1;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidDataType;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CounterActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;
    private static final Long PREVIOUS_TASK_ID = 455L;

    private ProcessInstanceTaskRepository processInstanceTaskRepository;
    private ProcessInstanceTaskEntity previousIterationTask;
    private CounterActionNodeV1 node;

    @BeforeEach
    void setUp() {
        previousIterationTask = null;
        processInstanceTaskRepository = proxy(ProcessInstanceTaskRepository.class, (methodName, args) -> switch (methodName) {
            case "findFirstByProcessInstanceIdAndProcessNodeIdAndIdNotOrderByStartedDesc",
                 "findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc" -> Optional.ofNullable(previousIterationTask);
            default -> unsupported(methodName);
        });
        node = new CounterActionNodeV1(processInstanceTaskRepository);
    }

    @Test
    void init_IncrementsConfiguredProcessDataPath() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "loop", Map.of("count", 2L),
                        "other", "value"
                ));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configuration("loop.count", 3L)),
                        processInstance(),
                        task(),
                        null,
                        processData,
                        nodeConfiguration(configuration("loop.count", 3L))
                ))
        );

        assertEquals("output", result.getViaPort());
        assertEquals(5L, result.getNodeData().get("value"));
        assertEquals(2L, result.getNodeData().get("previousValue"));
        assertEquals(3L, result.getNodeData().get("increment"));
        assertEquals("loop.count", result.getNodeData().get("storageTarget"));
        assertEquals("processData", result.getNodeData().get("storageMode"));

        @SuppressWarnings("unchecked")
        var loopData = (Map<String, Object>) result.getProcessData().get("loop");
        assertEquals(5L, loopData.get("count"));
        assertEquals("value", result.getProcessData().get("other"));
    }

    @Test
    void init_UsesPreviousNodeDataWhenVariablePathIsMissing() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of("other", "value"));

        previousIterationTask = previousIterationTask(Map.of("value", 4L));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configuration(null, 2L)),
                        processInstance(),
                        task(),
                        null,
                        processData,
                        nodeConfiguration(configuration(null, 2L))
                ))
        );

        assertEquals("output", result.getViaPort());
        assertNotNull(result.getProcessData());
        assertFalse(result.getProcessData().isEmpty());
        assertEquals("value", result.getProcessData().get("other"));
        assertEquals(6L, result.getNodeData().get("value"));
        assertEquals(4L, result.getNodeData().get("previousValue"));
        assertEquals(2L, result.getNodeData().get("increment"));
        assertEquals("nodeData", result.getNodeData().get("storageMode"));
    }

    @Test
    void init_StartsAtZeroWhenNoPreviousValueExists() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of("other", "value"));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configuration(null, null)),
                        processInstance(),
                        task(),
                        null,
                        processData,
                        nodeConfiguration(configuration(null, null))
                ))
        );

        assertEquals(1L, result.getNodeData().get("value"));
        assertEquals(0L, result.getNodeData().get("previousValue"));
        assertEquals(1L, result.getNodeData().get("increment"));
        assertEquals("nodeData", result.getNodeData().get("storageMode"));
        assertNotNull(result.getProcessData());
        assertFalse(result.getProcessData().isEmpty());
        assertEquals("value", result.getProcessData().get("other"));
    }

    @Test
    void init_RejectsNonNumericStoredValue() {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "loop", Map.of("count", "abc")
                ));

        assertThrows(
                ProcessNodeExecutionExceptionInvalidDataType.class,
                () -> node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configuration("loop.count", 1L)),
                        processInstance(),
                        task(),
                        null,
                        processData,
                        nodeConfiguration(configuration("loop.count", 1L))
                ))
        );
    }

    private static AuthoredElementValues configuration(String variablePath, Long increment) {
        var config = new AuthoredElementValues();
        if (variablePath != null) {
            config.put("variable", variablePath);
        }
        if (increment != null) {
            config.put("increment", increment);
        }
        return config;
    }

    private static ProcessNodeEntity processNode(AuthoredElementValues configuration) {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("Zähler")
                .setDataKey("counterNode")
                .setProcessNodeDefinitionKey("de.aivot.core.counter")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(configuration)
                .setOutputMappings(Map.of());
    }

    private static CounterActionNodeV1.CounterActionNodeV1Configuration nodeConfiguration(AuthoredElementValues configuration)
            throws ElementDataConversionException {
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.putAll(configuration);
        return ElementPOJOMapper.mapToPOJO(effectiveValues, CounterActionNodeV1.CounterActionNodeV1Configuration.class);
    }

    private static DerivedRuntimeElementData validationRuntime(AuthoredElementValues configuration) {
        var states = new ComputedElementStates();
        states.put("variable", new ComputedElementState());
        states.put("increment", new ComputedElementState());

        var effectiveValues = new EffectiveElementValues();
        effectiveValues.putAll(configuration);
        return new DerivedRuntimeElementData(effectiveValues, states);
    }

    private static ProcessInstanceEntity processInstance() {
        var now = Instant.now();

        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setAccessKey(UUID.randomUUID())
                .setProcessId(PROCESS_ID)
                .setInitialProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessInstanceStatus.Running)
                .setAssignedFileNumbers(List.of())
                .setIdentities(new IdentityDataMap())
                .setStarted(now)
                .setUpdated(now)
                .setInitialPayload(Map.of())
                .setInitialNodeId(1);
    }

    private static ProcessInstanceTaskEntity task() {
        return task(TASK_ID, Map.of(), Map.of(), Map.of());
    }

    private static ProcessInstanceTaskEntity previousIterationTask(Map<String, Object> runtimeData) {
        return task(PREVIOUS_TASK_ID, Map.of(), runtimeData, Map.of("other", "value"));
    }

    private static ProcessInstanceTaskEntity task(Long taskId,
                                                  Map<String, Object> runtimeData,
                                                  Map<String, Object> nodeData,
                                                  Map<String, Object> processData) {
        var now = Instant.now();

        return new ProcessInstanceTaskEntity()
                .setId(taskId)
                .setAccessKey(UUID.randomUUID())
                .setProcessInstanceId(PROCESS_INSTANCE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setProcessNodeId(NODE_ID)
                .setPreviousProcessInstanceTaskId(PREVIOUS_TASK_ID)
                .setPreviousProcessNodeId(77)
                .setPreviousProcessNodePortKey("output")
                .setStatus(ProcessTaskStatus.Running)
                .setStarted(now)
                .setUpdated(now)
                .setRuntimeData(runtimeData)
                .setNodeData(nodeData)
                .setProcessData(processData);
    }

    private static ProcessNodeExecutionLogger logger() {
        return new ProcessNodeExecutionLogger(
                PROCESS_INSTANCE_ID,
                TASK_ID,
                null,
                null,
                proxy(ProcessInstanceHistoryEventRepository.class, (methodName, args) -> switch (methodName) {
                    case "save" -> args[0];
                    default -> unsupported(methodName);
                })
        );
    }

    @FunctionalInterface
    private interface MethodHandler {
        Object invoke(String methodName, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, MethodHandler handler) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    var methodName = method.getName();
                    return switch (methodName) {
                        case "toString" -> type.getSimpleName() + "TestProxy";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> handler.invoke(methodName, args);
                    };
                }
        );
    }

    private static Object unsupported(String methodName) {
        throw new UnsupportedOperationException("Unexpected repository method call in test: " + methodName);
    }
}
