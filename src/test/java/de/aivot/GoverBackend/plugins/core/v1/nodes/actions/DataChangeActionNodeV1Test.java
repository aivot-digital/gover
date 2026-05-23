package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.GoverBackend.elements.models.elements.form.input.DateInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.DateTimeInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.DomainAndUserSelectInputElementValue;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.lib.DiffItem;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskUpdated;
import de.aivot.GoverBackend.process.models.TaskViewEvent;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.process.repositories.VPotentialProcessInstanceAccessRepository;
import de.aivot.GoverBackend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.GoverBackend.submission.services.ElementDataTransformService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static de.aivot.GoverBackend.TestData.authored;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataChangeActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;

    private TestAssignmentContextAssigneeResolverService assigneeResolverService;
    private DataChangeActionNodeV1 node;

    @BeforeEach
    void setUp() {
        assigneeResolverService = new TestAssignmentContextAssigneeResolverService();
        node = new DataChangeActionNodeV1(
                assigneeResolverService,
                new ElementDataTransformService(),
                derivationService()
        );
    }

    @Test
    void init_AssignsResolvedUserAndCopiesWorkingProcessData() throws Exception {
        assigneeResolverService.result = Optional.of("user-1");

        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "applicant", Map.of("name", "Ada"),
                        "meta", Map.of("source", "task")
                ));

        var result = node.init(new ProcessNodeExecutionInitContext(
                logger(),
                processNode(configuration()),
                processInstance("process-owner"),
                task(77, Map.of(), Map.of(), Map.of("applicant", Map.of("name", "Ada"))),
                null,
                processData,
                nodeConfiguration(configuration())
        ));

        var taskAssigned = assertInstanceOf(ProcessNodeExecutionResultTaskAssigned.class, result);
        assertEquals("user-1", taskAssigned.getAssignedUserId());
        assertEquals(
                Map.of(
                        "applicant", Map.of("name", "Ada"),
                        "meta", Map.of("source", "task")
                ),
                taskAssigned.getProcessData()
        );
        assertTrue(!taskAssigned.getProcessData().containsKey("$"));
    }

    @Test
    void getStaffTaskViewData_LoadsSavedDraftFromRuntimeData() throws Exception {
        var processData = Map.<String, Object>of("applicant", Map.of("name", "Ada"));

        var context = new ProcessNodeExecutionContextUIStaff(
                logger(),
                processNode(configuration()),
                processInstance("process-owner"),
                task(
                        77,
                        Map.of(
                                ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY,
                                authored("applicantName", "Grace")
                        ),
                        Map.of(),
                        processData
                ),
                null,
                user("staff-1"),
                nodeConfiguration(configuration()),
                currentProcessData(processData)
        );

        var layout = node.getStaffTaskView(context);
        var dataField = layout.findChild("applicantName", TextInputElement.class).orElseThrow();

        assertFalse(Boolean.TRUE.equals(dataField.getDisabled()));
        assertEquals(
                List.of(new TaskViewEvent("Aufgabe abschließen", "complete")),
                node.getStaffTaskViewEvents(context)
        );

        var data = node.getStaffTaskViewData(context);
        assertEquals("Grace", data.get("applicantName"));
    }

    @Test
    void onEventFromStaffTaskView_SaveIsRejectedAsUnknownEvent() {
        var processData = Map.<String, Object>of("applicant", Map.of("name", "Ada"));

        var ex = assertThrows(
                ResponseException.class,
                () -> node.onEventFromStaffTaskView(
                        new ProcessNodeExecutionContextUIStaff(
                                logger(),
                                processNode(configuration()),
                                processInstance("process-owner"),
                                task(
                                        77,
                                        Map.of(),
                                        Map.of("existing", "node-data"),
                                        processData
                                ),
                                null,
                                user("staff-1"),
                                nodeConfiguration(configuration()),
                                currentProcessData(processData)
                        ),
                        authored("applicantName", "Grace"),
                        "save"
                )
        );

        assertEquals("Unbekannte Aktion: save", ex.getMessage());
    }

    @Test
    void onAutoSaveFromStaffTaskView_PersistsDraftInRuntimeData() throws Exception {
        var processData = Map.<String, Object>of("applicant", Map.of("name", "Ada"));

        var result = node.onAutoSaveFromStaffTaskView(
                new ProcessNodeExecutionContextUIStaff(
                        logger(),
                        processNode(configuration()),
                        processInstance("process-owner"),
                        task(
                                77,
                                Map.of("keep", "value"),
                                Map.of("existing", "node-data"),
                                processData
                        ),
                        null,
                        user("staff-1"),
                        nodeConfiguration(configuration()),
                        currentProcessData(processData)
                ),
                authored("applicantName", "Grace")
        );

        assertTrue(result.isPresent());

        var updated = assertInstanceOf(ProcessNodeExecutionResultTaskUpdated.class, result.get());
        assertEquals("value", updated.getRuntimeData().get("keep"));
        assertEquals(Map.of("existing", "node-data"), updated.getNodeData());
        assertEquals(Map.of("applicant", Map.of("name", "Ada")), updated.getProcessData());

        var draftData = updated.getRuntimeData().get(ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY);
        assertNotNull(draftData);
        assertEquals("Grace", ((Map<?, ?>) draftData).get("applicantName"));
    }

    @Test
    void onEventFromStaffTaskView_CompleteMergesProcessDataAndStoresDiff() throws Exception {
        var processData = Map.<String, Object>of(
                "applicant", Map.of("name", "Ada", "age", 33),
                "untouched", "value"
        );

        var result = node.onEventFromStaffTaskView(
                new ProcessNodeExecutionContextUIStaff(
                        logger(),
                        processNode(configuration()),
                        processInstance("process-owner"),
                        task(
                                77,
                                Map.of(
                                ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY,
                                authored("applicantName", "Draft")
                        ),
                                Map.of(),
                                processData
                        ),
                        null,
                        user("staff-1"),
                        nodeConfiguration(configuration()),
                        currentProcessData(processData)
                ),
                authored("applicantName", "Grace"),
                "complete"
        );

        assertTrue(result.isPresent());

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result.get());
        assertEquals("output", completed.getViaPort());
        assertEquals(Map.of(), completed.getRuntimeData());
        assertEquals("staff-1", completed.getNodeData().get("processedByUserId"));
        assertNotNull(completed.getNodeData().get("processedAt"));

        @SuppressWarnings("unchecked")
        var applicant = (Map<String, Object>) completed.getProcessData().get("applicant");
        assertEquals("Grace", applicant.get("name"));
        assertEquals(33, applicant.get("age"));
        assertEquals("value", completed.getProcessData().get("untouched"));

        @SuppressWarnings("unchecked")
        var changedData = (Map<String, Object>) completed.getNodeData().get("data");
        assertEquals(Map.of("name", "Grace"), changedData.get("applicant"));

        @SuppressWarnings("unchecked")
        var diff = (List<DiffItem>) completed.getNodeData().get("diff");
        assertEquals(1, diff.size());
        assertEquals("applicant.name", diff.getFirst().field());
        assertEquals("Ada", diff.getFirst().oldValue());
        assertEquals("Grace", diff.getFirst().newValue());
    }

    @Test
    void onEventFromStaffTaskView_CompletePatchesArrayItemWithoutReplacingArray() throws Exception {
        var processData = Map.<String, Object>of(
                "members", List.of(
                        Map.of("name", "Ada", "age", 33),
                        Map.of("name", "Bob", "age", 41)
                ),
                "untouched", "value"
        );
        var configuration = configuration("members.0.name", null);

        var result = node.onEventFromStaffTaskView(
                new ProcessNodeExecutionContextUIStaff(
                        logger(),
                        processNode(configuration),
                        processInstance("process-owner"),
                        task(
                                77,
                                Map.of(),
                                Map.of(),
                                processData
                        ),
                        null,
                        user("staff-1"),
                        nodeConfiguration(configuration),
                        currentProcessData(processData)
                ),
                authored("applicantName", "Grace"),
                "complete"
        );

        assertTrue(result.isPresent());

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result.get());
        assertEquals("value", completed.getProcessData().get("untouched"));

        @SuppressWarnings("unchecked")
        var members = (List<Map<String, Object>>) completed.getProcessData().get("members");
        assertEquals(2, members.size());
        assertEquals("Grace", members.getFirst().get("name"));
        assertEquals(33, members.getFirst().get("age"));
        assertEquals(Map.of("name", "Bob", "age", 41), members.get(1));

        @SuppressWarnings("unchecked")
        var changedData = (Map<String, Object>) completed.getNodeData().get("data");
        assertEquals(List.of(Map.of("name", "Grace")), changedData.get("members"));

        @SuppressWarnings("unchecked")
        var diff = (List<DiffItem>) completed.getNodeData().get("diff");
        assertEquals(1, diff.size());
        assertEquals("members[0].name", diff.getFirst().field());
        assertEquals("Ada", diff.getFirst().oldValue());
        assertEquals("Grace", diff.getFirst().newValue());
    }

    @Test
    void onEventFromStaffTaskView_CompletePatchesReplicatingContainerRowsWithoutReplacingSiblingFields() throws Exception {
        var processData = Map.<String, Object>of(
                "members", List.of(
                        Map.of(
                                "name", "Ada",
                                "age", 33,
                                "address", Map.of("street", "Main Street 1")
                        ),
                        Map.of(
                                "name", "Bob",
                                "age", 41,
                                "address", Map.of("street", "Side Alley 2")
                        )
                ),
                "untouched", "value"
        );

        var memberName = new TextInputElement();
        memberName.setId("memberName");
        memberName.setLabel("Name");
        memberName.setDestinationKey("name");

        var members = new ReplicatingContainerLayoutElement();
        members.setId("membersEditor");
        members.setDestinationKey("members");
        members.setChildren(List.<BaseFormElement>of(memberName));

        var configuration = configuration(List.<BaseFormElement>of(members), null);

        var result = node.onEventFromStaffTaskView(
                new ProcessNodeExecutionContextUIStaff(
                        logger(),
                        processNode(configuration),
                        processInstance("process-owner"),
                        task(
                                77,
                                Map.of(),
                                Map.of(),
                                processData
                        ),
                        null,
                        user("staff-1"),
                        nodeConfiguration(configuration),
                        currentProcessData(processData)
                ),
                authored(
                        "membersEditor", List.of(
                                authored("memberName", "Grace"),
                                authored("memberName", "Bob")
                        )
                ),
                "complete"
        );

        assertTrue(result.isPresent());

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result.get());
        assertEquals("value", completed.getProcessData().get("untouched"));

        @SuppressWarnings("unchecked")
        var membersData = (List<Map<String, Object>>) completed.getProcessData().get("members");
        assertEquals(2, membersData.size());
        assertEquals("Grace", membersData.getFirst().get("name"));
        assertEquals(33, membersData.getFirst().get("age"));
        assertEquals(Map.of("street", "Main Street 1"), membersData.getFirst().get("address"));
        assertEquals(Map.of(
                "name", "Bob",
                "age", 41,
                "address", Map.of("street", "Side Alley 2")
        ), membersData.get(1));

        @SuppressWarnings("unchecked")
        var changedData = (Map<String, Object>) completed.getNodeData().get("data");
        assertEquals(
                List.of(
                        Map.of("name", "Grace"),
                        Map.of("name", "Bob")
                ),
                changedData.get("members")
        );

        @SuppressWarnings("unchecked")
        var diff = (List<DiffItem>) completed.getNodeData().get("diff");
        assertEquals(1, diff.size());
        assertEquals("members[0].name", diff.getFirst().field());
        assertEquals("Ada", diff.getFirst().oldValue());
        assertEquals("Grace", diff.getFirst().newValue());
    }

    @Test
    void onEventFromStaffTaskView_CompleteDoesNotCreateSpuriousDiffForEquivalentTemporalValues() throws Exception {
        var processData = Map.<String, Object>of(
                "date", "2026-05-09T00:00:00+02:00",
                "datetime", "2021-02-07T12:15:00+01:00"
        );

        var result = node.onEventFromStaffTaskView(
                new ProcessNodeExecutionContextUIStaff(
                        logger(),
                        processNode(configurationWithTemporalFields()),
                        processInstance("process-owner"),
                        task(
                                77,
                                Map.of(),
                                Map.of(),
                                processData
                        ),
                        null,
                        user("staff-1"),
                        nodeConfiguration(configurationWithTemporalFields()),
                        currentProcessData(processData)
                ),
                authored(
                        "dateField", "2026-05-08T22:00:00.000Z",
                        "dateTimeField", "2021-02-07T11:15:00.000Z"
                ),
                "complete"
        );

        assertTrue(result.isPresent());

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result.get());

        @SuppressWarnings("unchecked")
        var changedData = (Map<String, Object>) completed.getNodeData().get("data");
        assertEquals("2026-05-09T00:00:00+02:00", changedData.get("date"));
        assertEquals("2021-02-07T12:15:00+01:00", changedData.get("datetime"));

        @SuppressWarnings("unchecked")
        var diff = (List<DiffItem>) completed.getNodeData().get("diff");
        assertEquals(List.of(), diff);
    }

    private static AuthoredElementValues configuration() {
        return configuration(null);
    }

    private static AuthoredElementValues configuration(String taskDescription) {
        return configuration("applicant.name", taskDescription);
    }

    private static AuthoredElementValues configuration(String destinationKey, String taskDescription) {
        var valueField = new TextInputElement();
        valueField.setId("applicantName");
        valueField.setLabel("Name");
        valueField.setDestinationKey(destinationKey);

        return configuration(List.of(valueField), taskDescription);
    }

    private static AuthoredElementValues configuration(List<? extends BaseFormElement> children, String taskDescription) {
        var contentRoot = new GroupLayoutElement();
        contentRoot.setId("data-change-root");
        contentRoot.setChildren(new java.util.ArrayList<BaseFormElement>(children));

        var configuration = new AuthoredElementValues();
        configuration.put("data_definition", contentRoot);
        configuration.put("assignment_context", assignmentContext());
        if (taskDescription != null) {
            configuration.put("task_description", taskDescription);
        }

        return configuration;
    }

    private static AuthoredElementValues configurationWithTemporalFields() {
        var contentRoot = new GroupLayoutElement();
        contentRoot.setId("data-change-root");

        var dateField = new DateInputElement();
        dateField.setId("dateField");
        dateField.setLabel("Datum");
        dateField.setDestinationKey("date");

        var dateTimeField = new DateTimeInputElement();
        dateTimeField.setId("dateTimeField");
        dateTimeField.setLabel("Datum und Uhrzeit");
        dateTimeField.setDestinationKey("datetime");

        contentRoot.setChildren(List.of(dateField, dateTimeField));

        var configuration = new AuthoredElementValues();
        configuration.put("data_definition", contentRoot);
        configuration.put("assignment_context", assignmentContext());
        return configuration;
    }

    private static AssignmentContextInputElementValue assignmentContext() {
        return new AssignmentContextInputElementValue()
                .setDomainAndUserSelection(List.of(new DomainAndUserSelectInputElementValue("user", "user-1")))
                .setPreferPreviousTaskAssignee(false)
                .setPreferUninvolvedUser(false)
                .setPreferProcessInstanceAssignee(false);
    }

    private static ElementDerivationService derivationService() {
        return new ElementDerivationService(
                new JavascriptEngineFactoryService(List.of()),
                new NoCodeEvaluationService(List.of()),
                new ElementDataTransformService()
        );
    }

    private static ProcessNodeEntity processNode(AuthoredElementValues configuration) {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("Daten ändern")
                .setDataKey("dataChangeNode")
                .setProcessNodeDefinitionKey("de.aivot.core.data_change")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(configuration)
                .setOutputMappings(Map.of());
    }

    private static DataChangeActionNodeV1.DataChangeActionNodeConfig nodeConfiguration(AuthoredElementValues configuration)
            throws ElementDataConversionException {
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.putAll(configuration);
        return ElementPOJOMapper.mapToPOJO(effectiveValues, DataChangeActionNodeV1.DataChangeActionNodeConfig.class);
    }

    private static ProcessExecutionData currentProcessData(Map<String, Object> processData) {
        return new ProcessExecutionData().addProcessData(processData);
    }

    private static ProcessInstanceEntity processInstance(String assignedUserId) {
        var now = Instant.now();

        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setAccessKey(UUID.randomUUID())
                .setProcessId(PROCESS_ID)
                .setInitialProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessInstanceStatus.Running)
                .setAssignedUserId(assignedUserId)
                .setAssignedFileNumbers(List.of())
                .setIdentities(Map.of())
                .setStarted(now)
                .setUpdated(now)
                .setInitialPayload(Map.of())
                .setInitialNodeId(1);
    }

    private static ProcessInstanceTaskEntity task(
            Integer previousProcessNodeId,
            Map<String, Object> runtimeData,
            Map<String, Object> nodeData,
            Map<String, Object> processData
    ) {
        var now = Instant.now();

        return new ProcessInstanceTaskEntity()
                .setId(TASK_ID)
                .setAccessKey(UUID.randomUUID())
                .setProcessInstanceId(PROCESS_INSTANCE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setProcessNodeId(NODE_ID)
                .setPreviousProcessInstanceTaskId(null)
                .setPreviousProcessNodeId(previousProcessNodeId)
                .setPreviousProcessNodePortKey(null)
                .setStatus(ProcessTaskStatus.Running)
                .setAssignedUserId("staff-1")
                .setStarted(now)
                .setUpdated(now)
                .setRuntimeData(runtimeData)
                .setNodeData(nodeData)
                .setProcessData(processData);
    }

    private static UserEntity user(String userId) {
        return new UserEntity()
                .setId(userId)
                .setEmail("staff@example.org")
                .setFirstName("Staff")
                .setLastName("User")
                .setEnabled(true)
                .setVerified(true)
                .setDeletedInIdp(false);
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

    private static class TestAssignmentContextAssigneeResolverService extends AssignmentContextAssigneeResolverService {
        private Optional<String> result = Optional.empty();

        private TestAssignmentContextAssigneeResolverService() {
            super(
                    proxy(VPotentialProcessInstanceAccessRepository.class, (methodName, args) -> unsupported(methodName)),
                    proxy(ProcessInstanceTaskRepository.class, (methodName, args) -> unsupported(methodName))
            );
        }

        @Override
        public Optional<String> resolveAssignee(Integer processId,
                                                Integer processVersion,
                                                Long processInstanceId,
                                                Integer previousProcessNodeId,
                                                String processInstanceAssignedUserId,
                                                AssignmentContextInputElementValue assignmentContext,
                                                List<String> requiredPermissions) {
            return result;
        }
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
