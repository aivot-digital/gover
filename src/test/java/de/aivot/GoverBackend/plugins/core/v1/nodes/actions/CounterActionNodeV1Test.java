package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.ComputedElementState;
import de.aivot.GoverBackend.elements.models.ComputedElementStates;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextInit;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static de.aivot.GoverBackend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                node.init(new ProcessNodeExecutionContextInit(
                        logger(),
                        processNode(configuration("loop.count", 3L)),
                        processInstance(),
                        task(),
                        null,
                        processData,
                        runtime(configuration("loop.count", 3L))
                ))
        );

        assertEquals("output", result.getViaPort());
        assertEquals(5L, result.getNodeData().get("value"));
        assertEquals(2L, result.getNodeData().get("previousValue"));
        assertEquals(3L, result.getNodeData().get("increment"));
        assertEquals("loop.count", result.getNodeData().get("storageTarget"));

        @SuppressWarnings("unchecked")
        var loopData = (Map<String, Object>) result.getProcessData().get("loop");
        assertEquals(5L, loopData.get("count"));
        assertEquals("value", result.getProcessData().get("other"));
    }

    @Test
    void init_UsesPreviousNodeDataWhenVariablePathIsMissing() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of("other", "value"));
        processData.put("_counterNode", Map.of());
        previousIterationTask = previousIterationTask(Map.of("value", 4L));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionContextInit(
                        logger(),
                        processNode(configuration(null, 2L)),
                        processInstance(),
                        task(),
                        null,
                        processData,
                        runtime(configuration(null, 2L))
                ))
        );

        assertEquals("output", result.getViaPort());
        assertNull(result.getProcessData());
        assertEquals(6L, result.getNodeData().get("value"));
        assertEquals(4L, result.getNodeData().get("previousValue"));
        assertEquals(2L, result.getNodeData().get("increment"));
        assertEquals("_counterNode.value", result.getNodeData().get("storageTarget"));
    }

    @Test
    void init_StartsAtZeroWhenNoPreviousValueExists() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of("other", "value"));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionContextInit(
                        logger(),
                        processNode(configuration(null, null)),
                        processInstance(),
                        task(),
                        null,
                        processData,
                        runtime(configuration(null, null))
                ))
        );

        assertEquals(1L, result.getNodeData().get("value"));
        assertEquals(0L, result.getNodeData().get("previousValue"));
        assertEquals(1L, result.getNodeData().get("increment"));
        assertNull(result.getProcessData());
    }

    @Test
    void init_RejectsNonNumericStoredValue() {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "loop", Map.of("count", "abc")
                ));

        assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(new ProcessNodeExecutionContextInit(
                        logger(),
                        processNode(configuration("loop.count", 1L)),
                        processInstance(),
                        task(),
                        null,
                        processData,
                        runtime(configuration("loop.count", 1L))
                ))
        );
    }

    @Test
    void validateConfiguration_RejectsInvalidIncrement() {
        var exception = assertThrows(
                ResponseException.class,
                () -> node.validateConfiguration(
                        processNode(configuration("loop.count", 0L)),
                        configuration("loop.count", 0L),
                        validationRuntime(configuration("loop.count", 0L))
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getDetails() instanceof DerivedRuntimeElementData);

        var details = (DerivedRuntimeElementData) exception.getDetails();
        assertTrue(details.getElementStates().get("increment").getError().contains("mindestens 1"));
    }

    @Test
    void validateConfiguration_RejectsInvalidVariablePath() {
        var config = configuration("loop..count", 1L);

        var exception = assertThrows(
                ResponseException.class,
                () -> node.validateConfiguration(processNode(config), config, validationRuntime(config))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getDetails() instanceof DerivedRuntimeElementData);

        var details = (DerivedRuntimeElementData) exception.getDetails();
        assertTrue(details.getElementStates().get("variable").getError().contains("Ungültiger Pfad"));
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

    private static DerivedRuntimeElementData validationRuntime(AuthoredElementValues configuration) {
        var states = new ComputedElementStates();
        states.put("variable", new ComputedElementState());
        states.put("increment", new ComputedElementState());

        var effectiveValues = new EffectiveElementValues();
        effectiveValues.putAll(configuration);
        return new DerivedRuntimeElementData(effectiveValues, states);
    }

    private static ProcessInstanceEntity processInstance() {
        var now = LocalDateTime.now();

        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setAccessKey(UUID.randomUUID())
                .setProcessId(PROCESS_ID)
                .setInitialProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessInstanceStatus.Running)
                .setAssignedFileNumbers(List.of())
                .setIdentities(Map.of())
                .setStarted(now)
                .setUpdated(now)
                .setInitialPayload(Map.of())
                .setInitialNodeId(1);
    }

    private static ProcessInstanceTaskEntity task() {
        return task(TASK_ID, Map.of(), Map.of());
    }

    private static ProcessInstanceTaskEntity previousIterationTask(Map<String, Object> nodeData) {
        return task(PREVIOUS_TASK_ID, nodeData, Map.of("other", "value"));
    }

    private static ProcessInstanceTaskEntity task(Long taskId,
                                                  Map<String, Object> nodeData,
                                                  Map<String, Object> processData) {
        var now = LocalDateTime.now();

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
                .setRuntimeData(Map.of())
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
