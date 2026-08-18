package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.nocode.models.NoCodeOperand;
import de.aivot.gover.backend.nocode.models.NoCodeProcessDataReference;
import de.aivot.gover.backend.nocode.models.NoCodeStaticValue;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.gover.backend.plugins.core.v1.nodes.actions.NoCodeActionNodeV1;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.gover.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.gover.backend.utils.ApplicationTimeZone;
import de.aivot.gover.backend.elements.models.elements.form.input.NoCodeInputElementItem;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
    private ZoneId originalZone;

    @BeforeEach
    void setUp() {
        originalZone = ApplicationTimeZone.getZoneId();
        ApplicationTimeZone.configure(ZoneId.of("Europe/Berlin"));
        node = new NoCodeActionNodeV1(new NoCodeEvaluationService(List.of()));
    }

    @AfterEach
    void tearDown() {
        ApplicationTimeZone.configure(originalZone);
    }

    @Test
    void getMetadata_ShouldForwardTrimmedVariableNamesWithoutFilteringDuplicates() {
        var processNode = processNode();
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

        var metadata = node.getMetadata(
                processNode,
                configuration,
                ProcessNodeDefinitionMetadata
                        .empty()
                        .addForwardedProcessDataKey("existing", "existing", null, processNode)
        );

        assertEquals(
                List.of(
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("existing", "existing", null, processNode),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("result.total", "result.total", null, processNode),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("result.date", "result.date", null, processNode),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("result..invalid", "result..invalid", null, processNode),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("result.total", "result.total", null, processNode),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("items.0.name", "items.0.name", null, processNode),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("items[0].name", "items[0].name", null, processNode),
                        new ProcessNodeDefinitionMetadata.ForwardedProcessDataKey("items[*].name", "items[*].name", null, processNode)
                ),
                metadata.forwardedProcessDataKeys()
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

    @Test
    void init_ShouldPreserveBusinessLocalDateWhenCastingOffsetTimestampStringToDate() throws Exception {
        var result = init(variable(
                "localDate",
                "date",
                new NoCodeStaticValue("2026-05-09T00:00:00+02:00")
        ));

        assertEquals("2026-05-09", result.getProcessData().get("localDate"));

        @SuppressWarnings("unchecked")
        var variables = (List<Map<String, Object>>) result.getNodeData().get("variables");
        assertEquals("2026-05-09", variables.getFirst().get("value"));
    }

    @Test
    void init_ShouldPreserveBusinessLocalDateWhenCastingInstantToDate() throws Exception {
        var result = init(variable(
                "localDate",
                "date",
                new NoCodeStaticValue(Instant.parse("2026-05-08T22:00:00Z"))
        ));

        assertEquals("2026-05-09", result.getProcessData().get("localDate"));
    }

    @Test
    void init_ShouldUseBusinessTimezoneWhenCastingLocalDateToDateTime() throws Exception {
        var result = init(variable(
                "appointment",
                "datetime",
                new NoCodeStaticValue(LocalDate.of(2026, 5, 9))
        ));

        assertEquals("2026-05-09T00:00:00+02:00", result.getProcessData().get("appointment"));
    }

    @Test
    void init_ShouldUseBusinessTimezoneWhenCastingLocalDateTimeToDateTime() throws Exception {
        var result = init(variable(
                "appointment",
                "datetime",
                new NoCodeStaticValue(LocalDateTime.of(2026, 5, 9, 8, 30))
        ));

        assertEquals("2026-05-09T08:30:00+02:00", result.getProcessData().get("appointment"));
    }

    @Test
    void init_ShouldUseBusinessTimezoneWhenCastingLocalDateTimeToString() throws Exception {
        var result = init(variable(
                "appointment",
                "string",
                new NoCodeStaticValue(LocalDateTime.of(2026, 5, 9, 8, 30))
        ));

        assertEquals("2026-05-09T08:30:00+02:00", result.getProcessData().get("appointment"));
    }

    @Test
    void init_ShouldKeepTimeValuesZoneFree() throws Exception {
        var result = init(variable(
                "openingTime",
                "time",
                new NoCodeStaticValue(LocalTime.of(8, 30, 15))
        ));

        assertEquals("08:30:15", result.getProcessData().get("openingTime"));
    }

    @Test
    void init_ShouldCanonicalizeTimeValuesWithZeroSeconds() throws Exception {
        var result = init(
                variable("typedTime", "time", new NoCodeStaticValue("08:30")),
                variable("stringTime", "string", new NoCodeStaticValue(LocalTime.of(8, 30)))
        );

        assertEquals("08:30:00", result.getProcessData().get("typedTime"));
        assertEquals("08:30:00", result.getProcessData().get("stringTime"));
    }

    @Test
    void init_ShouldRejectNonexistentApplicationLocalDateTime() {
        assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> init(variable(
                        "appointment",
                        "datetime",
                        new NoCodeStaticValue(LocalDateTime.of(2026, 3, 29, 2, 30))
                ))
        );
    }

    @Nonnull
    private ProcessNodeExecutionResultTaskCompleted init(NoCodeActionNodeV1.NoCodeActionNodeVariableConfiguration... variables) throws Exception {
        return assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(new ProcessNodeExecutionInitContext<>(
                        logger(),
                        processNode(),
                        processInstance(),
                        task(),
                        null,
                        new ProcessExecutionData(),
                        configuration(variables)
                ))
        );
    }

    private static NoCodeActionNodeV1.NoCodeActionNodeConfiguration configuration(NoCodeActionNodeV1.NoCodeActionNodeVariableConfiguration... variables) {
        var configuration = new NoCodeActionNodeV1.NoCodeActionNodeConfiguration();
        configuration.variables = Arrays.stream(variables).toList();
        return configuration;
    }

    private static NoCodeActionNodeV1.NoCodeActionNodeVariableConfiguration variable(String name,
                                                                                      NoCodeOperand expression) {
        return variable(name, "any", expression);
    }

    private static NoCodeActionNodeV1.NoCodeActionNodeVariableConfiguration variable(String name,
                                                                                      String targetType,
                                                                                      NoCodeOperand expression) {
        var variable = new NoCodeActionNodeV1.NoCodeActionNodeVariableConfiguration();
        variable.name = name;
        variable.targetType = targetType;
        if (expression != null) {
            variable.expression = new NoCodeInputElementItem(expression);
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
        var now = Instant.now();

        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setAccessKey(UUID.randomUUID().toString())
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
        var now = Instant.now();

        return new ProcessInstanceTaskEntity()
                .setId(TASK_ID)
                .setAccessKey(UUID.randomUUID().toString())
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
