package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DataMappingActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;

    private DataMappingActionNodeV1 node;

    @BeforeEach
    void setUp() {
        node = new DataMappingActionNodeV1();
    }

    @Test
    void init_MapsMissingSourcePathToNullWithoutFailing() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of("existing", "value"));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configuration(List.of(Map.of(
                                "source", "missing.path",
                                "target", "mapped.value"
                        )), false)),
                        processInstance(),
                        task(Map.of()),
                        null,
                        processData,
                        nodeConfiguration(configuration(List.of(Map.of(
                                "source", "missing.path",
                                "target", "mapped.value"
                        )), false))
                ))
        );

        assertEquals("value", result.getProcessData().get("existing"));

        @SuppressWarnings("unchecked")
        var mapped = (Map<String, Object>) result.getProcessData().get("mapped");
        assertNotNull(mapped);
        assertTrue(mapped.containsKey("value"));
        assertNull(mapped.get("value"));

        @SuppressWarnings("unchecked")
        var mappedValues = (List<Map<String, Object>>) result.getNodeData().get("mappedValues");
        assertNull(mappedValues.getFirst().get("mapped"));
    }

    @Test
    void init_RenamesValueAndPrunesEmptySourceContainers() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "person", Map.of("firstName", "Ada")
                ));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configuration(List.of(Map.of(
                                "source", "person.firstName",
                                "target", "applicant.firstName",
                                "cleanupSource", true
                        )), true)),
                        processInstance(),
                        task(Map.of()),
                        null,
                        processData,
                        nodeConfiguration(configuration(List.of(Map.of(
                                "source", "person.firstName",
                                "target", "applicant.firstName",
                                "cleanupSource", true
                        )), true))
                ))
        );

        assertFalse(result.getProcessData().containsKey("person"));

        @SuppressWarnings("unchecked")
        var applicant = (Map<String, Object>) result.getProcessData().get("applicant");
        assertEquals("Ada", applicant.get("firstName"));
    }

    @Test
    void init_DeletesValueWithoutCopyAndKeepsEmptyContainersWhenConfigured() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "person", Map.of("middleName", "Byron")
                ));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configuration(List.of(Map.of(
                                "source", "person.middleName",
                                "deleteOnly", true
                        )), false)),
                        processInstance(),
                        task(Map.of()),
                        null,
                        processData,
                        nodeConfiguration(configuration(List.of(Map.of(
                                "source", "person.middleName",
                                "deleteOnly", true
                        )), false))
                ))
        );

        @SuppressWarnings("unchecked")
        var person = (Map<String, Object>) result.getProcessData().get("person");
        assertNotNull(person);
        assertTrue(person.isEmpty());
    }

    @Test
    void init_MapsWildcardSourceAndTargetPathsUsingMatchingIndices() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "personen", List.of(
                                Map.of("name", "Renate", "alter", 22),
                                Map.of("name", "Gerda", "alter", 41)
                        )
                ));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configuration(List.of(Map.of(
                                "source", "personen.*.alter",
                                "target", "personen.*.alterNeu"
                        )), false)),
                        processInstance(),
                        task(Map.of()),
                        null,
                        processData,
                        nodeConfiguration(configuration(List.of(Map.of(
                                "source", "personen.*.alter",
                                "target", "personen.*.alterNeu"
                        )), false))
                ))
        );

        @SuppressWarnings("unchecked")
        var personen = (List<Map<String, Object>>) result.getProcessData().get("personen");
        assertEquals(2, personen.size());
        assertEquals("Renate", personen.getFirst().get("name"));
        assertEquals(22, personen.getFirst().get("alter"));
        assertEquals(22, personen.getFirst().get("alterNeu"));
        assertEquals("Gerda", personen.get(1).get("name"));
        assertEquals(41, personen.get(1).get("alter"));
        assertEquals(41, personen.get(1).get("alterNeu"));
    }

    @Test
    void init_DeletesWildcardSourceAndPrunesEmptyContainers() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "personen", List.of(
                                Map.of("alter", 22),
                                Map.of("alter", 41)
                        )
                ));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configuration(List.of(Map.of(
                                "source", "personen.*.alter",
                                "deleteOnly", true
                        )), true)),
                        processInstance(),
                        task(Map.of()),
                        null,
                        processData,
                        nodeConfiguration(configuration(List.of(Map.of(
                                "source", "personen.*.alter",
                                "deleteOnly", true
                        )), true))
                ))
        );

        assertEquals(Map.of(), result.getProcessData());
    }

    @Test
    void init_CleansUpWildcardSourceAfterCopyWithoutRemovingTargetValues() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "personen", List.of(
                                Map.of("name", "Renate", "alter", 22),
                                Map.of("name", "Gerda", "alter", 41)
                        )
                ));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configuration(List.of(Map.of(
                                "source", "personen.*.alter",
                                "target", "personen.*.alterNeu",
                                "cleanupSource", true
                        )), true)),
                        processInstance(),
                        task(Map.of()),
                        null,
                        processData,
                        nodeConfiguration(configuration(List.of(Map.of(
                                "source", "personen.*.alter",
                                "target", "personen.*.alterNeu",
                                "cleanupSource", true
                        )), true))
                ))
        );

        @SuppressWarnings("unchecked")
        var personen = (List<Map<String, Object>>) result.getProcessData().get("personen");
        assertEquals(
                List.of(
                        Map.of("name", "Renate", "alterNeu", 22),
                        Map.of("name", "Gerda", "alterNeu", 41)
                ),
                personen
        );
    }

    @Test
    void init_DoesNotDeleteValueWhenCleanupSourceIsEnabledForSameSourceAndTarget() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "person", Map.of("firstName", "Ada")
                ));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configuration(List.of(Map.of(
                                "source", "person.firstName",
                                "target", "person.firstName",
                                "cleanupSource", true
                        )), true)),
                        processInstance(),
                        task(Map.of()),
                        null,
                        processData,
                        nodeConfiguration(configuration(List.of(Map.of(
                                "source", "person.firstName",
                                "target", "person.firstName",
                                "cleanupSource", true
                        )), true))
                ))
        );

        @SuppressWarnings("unchecked")
        var person = (Map<String, Object>) result.getProcessData().get("person");
        assertEquals(Map.of("firstName", "Ada"), person);

        @SuppressWarnings("unchecked")
        var mappedValues = (List<Map<String, Object>>) result.getNodeData().get("mappedValues");
        assertEquals("person.firstName", mappedValues.getFirst().get("originalPath"));
        assertEquals("person.firstName", mappedValues.getFirst().get("newPath"));
        assertEquals("Ada", mappedValues.getFirst().get("mapped"));
    }

    @Test
    void init_FailsForInvalidRawSourcePathDuringExecution() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "personen", List.of(
                                Map.of("alter", 22)
                        )
                ));

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configuration(List.of(Map.of(
                                "source", "personen[0].alter",
                                "target", "personen.0.alterNeu"
                        )), false)),
                        processInstance(),
                        task(Map.of()),
                        null,
                        processData,
                        nodeConfiguration(configuration(List.of(Map.of(
                                "source", "personen[0].alter",
                                "target", "personen.0.alterNeu"
                        )), false))
                ))
        );

        assertTrue(exception.getMessage().contains("Zeile 1"));
    }

    @Test
    void init_FailsForWildcardMismatchDuringExecution() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "personen", List.of(
                                Map.of("alter", 22),
                                Map.of("alter", 41)
                        )
                ));

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configuration(List.of(Map.of(
                                "source", "personen.*.alter",
                                "target", "personen.alterNeu"
                        )), false)),
                        processInstance(),
                        task(Map.of()),
                        null,
                        processData,
                        nodeConfiguration(configuration(List.of(Map.of(
                                "source", "personen.*.alter",
                                "target", "personen.alterNeu"
                        )), false))
                ))
        );

        assertTrue(exception.getMessage().contains("Zeile 1"));
    }

    private static AuthoredElementValues configuration(List<Map<String, Object>> rules, boolean cleanupEmptyContainers) {
        var config = new AuthoredElementValues();
        config.put("rules", rules);
        config.put("cleanupEmptyContainers", cleanupEmptyContainers);
        return config;
    }

    private static DataMappingActionNodeV1.DataMappingActionNodeV1Config nodeConfiguration(AuthoredElementValues configuration)
            throws ElementDataConversionException {
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.putAll(configuration);
        return ElementPOJOMapper.mapToPOJO(effectiveValues, DataMappingActionNodeV1.DataMappingActionNodeV1Config.class);
    }

    private static ProcessNodeEntity processNode(AuthoredElementValues configuration) {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("Daten abbilden")
                .setDataKey("dataMappingNode")
                .setProcessNodeDefinitionKey("de.aivot.core.data_mapping")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(configuration)
                .setOutputMappings(Map.of());
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

    private static ProcessInstanceTaskEntity task(Map<String, Object> processData) {
        var now = LocalDateTime.now();

        return new ProcessInstanceTaskEntity()
                .setId(TASK_ID)
                .setAccessKey(UUID.randomUUID())
                .setProcessInstanceId(PROCESS_INSTANCE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setProcessNodeId(NODE_ID)
                .setPreviousProcessInstanceTaskId(455L)
                .setPreviousProcessNodeId(77)
                .setPreviousProcessNodePortKey("output")
                .setStatus(ProcessTaskStatus.Running)
                .setStarted(now)
                .setUpdated(now)
                .setRuntimeData(Map.of())
                .setNodeData(Map.of())
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
