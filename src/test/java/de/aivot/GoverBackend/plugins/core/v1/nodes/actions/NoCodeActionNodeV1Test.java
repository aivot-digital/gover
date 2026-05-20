package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.nocode.models.NoCodeOperand;
import de.aivot.GoverBackend.nocode.models.NoCodeProcessDataReference;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.models.ProcessDataKeyHint;
import de.aivot.GoverBackend.process.models.ProcessDataKeyHintType;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;

    private NoCodeActionNodeV1 node;

    @BeforeEach
    void setUp() {
        node = new NoCodeActionNodeV1(new NoCodeEvaluationService(List.of()));
    }

    @Test
    void calculateProcessDataKeyHints_ShouldKeepOnlyStrictDestinationKeys() {
        var configuration = configuration(
                variable("result.total", null),
                variable(" result.date ", null),
                variable("result..invalid", null),
                variable("result.total", null),
                variable("items.0.name", null),
                variable("items[0].name", null),
                variable("items[*].name", null),
                variable(" ", null),
                null
        );

        var hints = node.calculateProcessDataKeyHints(
                processNode(),
                configuration,
                List.of(new ProcessDataKeyHint("existing", ProcessDataKeyHintType.ProcessData))
        );

        assertEquals(
                List.of(
                        new ProcessDataKeyHint("existing", ProcessDataKeyHintType.ProcessData),
                        new ProcessDataKeyHint("result.total", ProcessDataKeyHintType.ProcessData),
                        new ProcessDataKeyHint("result.date", ProcessDataKeyHintType.ProcessData),
                        new ProcessDataKeyHint("items.0.name", ProcessDataKeyHintType.ProcessData)
                ),
                hints
        );
    }

    @Test
    void init_ShouldWriteWildcardTargetsUsingMatchingIndices() throws Exception {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "personen", List.of(
                                Map.of("name", "Renate", "alter", 22),
                                Map.of("name", "Gerda", "alter", 41)
                        )
                ));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionInitContext<>(
                        logger(),
                        processNode(),
                        processInstance(),
                        task(),
                        null,
                        processData,
                        configuration(
                                variable(
                                        "personen.*.alterNeu",
                                        new NoCodeProcessDataReference("personen.*.alter")
                                )
                        )
                ))
        );

        @SuppressWarnings("unchecked")
        var personen = (List<Map<String, Object>>) result.getProcessData().get("personen");
        assertEquals(22, personen.getFirst().get("alterNeu"));
        assertEquals(41, personen.get(1).get("alterNeu"));

        @SuppressWarnings("unchecked")
        var variables = (List<Map<String, Object>>) result.getNodeData().get("variables");
        assertEquals(2, variables.size());
        assertEquals("personen.*.alterNeu", variables.getFirst().get("configuredPath"));
        assertEquals("personen.0.alterNeu", variables.getFirst().get("resolvedPath"));
        assertEquals(List.of(0), variables.getFirst().get("wildcardIndices"));
        assertEquals(22, variables.getFirst().get("value"));
        assertEquals("personen.1.alterNeu", variables.get(1).get("resolvedPath"));
        assertEquals(List.of(1), variables.get(1).get("wildcardIndices"));
        assertEquals(41, variables.get(1).get("value"));
        assertEquals(2, result.getNodeData().get("variableCount"));
    }

    @Test
    void init_ShouldRejectImplicitWildcardReferenceWithoutBinding() {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "personen", List.of(
                                Map.of("alter", 22)
                        )
                ));

        var ex = assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(new ProcessNodeExecutionInitContext<>(
                        logger(),
                        processNode(),
                        processInstance(),
                        task(),
                        null,
                        processData,
                        configuration(
                                variable(
                                        "result.alter",
                                        new NoCodeProcessDataReference("personen.*.alter")
                                )
                        )
                ))
        );

        assertTrue(ex.getMessage().contains("Wildcard"));
    }

    private static NoCodeActionNodeV1.NoCodeActionNodeConfiguration configuration(NoCodeActionNodeV1.NoCodeActionNodeVariableConfiguration... variables) {
        var configuration = new NoCodeActionNodeV1.NoCodeActionNodeConfiguration();
        configuration.variables = Arrays.stream(variables).toList();
        return configuration;
    }

    private static NoCodeActionNodeV1.NoCodeActionNodeVariableConfiguration variable(String name,
                                                                                      NoCodeOperand expression) {
        var variable = new NoCodeActionNodeV1.NoCodeActionNodeVariableConfiguration();
        variable.name = name;
        variable.targetType = "any";
        if (expression != null) {
            variable.expression = new de.aivot.GoverBackend.elements.models.elements.form.input.NoCodeInputElementItem(expression);
        }
        return variable;
    }

    private static ProcessNodeEntity processNode() {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("No-Code")
                .setDataKey("noCode")
                .setProcessNodeDefinitionKey("de.aivot.core.no-code")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
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

    private static ProcessInstanceTaskEntity task() {
        var now = LocalDateTime.now();

        return new ProcessInstanceTaskEntity()
                .setId(TASK_ID)
                .setAccessKey(UUID.randomUUID())
                .setProcessInstanceId(PROCESS_INSTANCE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setProcessNodeId(NODE_ID)
                .setStatus(ProcessTaskStatus.Running)
                .setStarted(now)
                .setUpdated(now)
                .setRuntimeData(Map.of())
                .setNodeData(Map.of())
                .setProcessData(Map.of());
    }

    private static ProcessNodeExecutionLogger logger() {
        var repository = (ProcessInstanceHistoryEventRepository) Proxy.newProxyInstance(
                ProcessInstanceHistoryEventRepository.class.getClassLoader(),
                new Class[]{ProcessInstanceHistoryEventRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> args[0];
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "ProcessInstanceHistoryEventRepositoryProxy";
                    default -> null;
                }
        );
        return new ProcessNodeExecutionLogger(
                PROCESS_INSTANCE_ID,
                TASK_ID,
                null,
                null,
                repository
        );
    }
}
