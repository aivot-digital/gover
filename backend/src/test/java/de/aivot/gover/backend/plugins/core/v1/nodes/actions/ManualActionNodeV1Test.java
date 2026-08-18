package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import de.aivot.gover.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.EffectiveElementValues;
import de.aivot.gover.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.gover.backend.elements.models.elements.form.input.*;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.gover.backend.elements.services.CodeListElementOptionsService;
import de.aivot.gover.backend.elements.services.ElementDerivationService;
import de.aivot.gover.backend.elements.utils.ElementPOJOMapper;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.models.ProcessNodeDefinition;
import de.aivot.gover.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.gover.backend.process.models.TaskViewEvent;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskUpdated;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.gover.backend.process.repositories.VPotentialProcessInstanceAccessRepository;
import de.aivot.gover.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.gover.backend.process.services.TemplateRenderService;
import de.aivot.gover.backend.submission.services.ElementDataTransformService;
import de.aivot.gover.backend.user.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.*;

import static de.aivot.gover.backend.TestData.authored;
import static org.junit.jupiter.api.Assertions.*;

class ManualActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;

    private TestAssignmentContextAssigneeResolverService assigneeResolverService;
    private ManualActionNodeV1 node;

    @BeforeEach
    void setUp() {
        assigneeResolverService = new TestAssignmentContextAssigneeResolverService();
        node = new ManualActionNodeV1(
                assigneeResolverService,
                new ElementDataTransformService(),
                derivationService(),
                new TemplateRenderService(new JavascriptEngineFactoryService(List.of()))
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
        assertEquals(PROCESS_ID, assigneeResolverService.processId);
        assertEquals(PROCESS_VERSION, assigneeResolverService.processVersion);
        assertEquals(PROCESS_INSTANCE_ID, assigneeResolverService.processInstanceId);
        assertEquals(77, assigneeResolverService.previousProcessNodeId);
        assertEquals("process-owner", assigneeResolverService.processInstanceAssignedUserId);
        assertEquals(assignmentContext(), assigneeResolverService.assignmentContext);
        assertEquals(List.of("process_instance.edit_task"), assigneeResolverService.requiredPermissions);
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
                                authored(
                                        "applicantName", "Grace",
                                        "manualActionRemark", "<p>Entwurf gespeichert.</p>"
                                )
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
        var description = layout.findChild("manual-action-description-content", RichTextContentElement.class).orElseThrow();
        var dataField = layout.findChild("applicantName", TextInputElement.class).orElseThrow();
        var remarkField = layout.findChild("manualActionRemark", RichTextInputElement.class).orElseThrow();

        assertEquals("<p>Bitte führen Sie die Prüfung vor Ort durch.</p>", description.getContent());
        assertFalse(Boolean.TRUE.equals(dataField.getDisabled()));
        assertTrue(layout.findChild("manual-action-remark-spacer").isPresent());
        assertEquals(6.0, remarkField.getWeight());
        assertTrue(layout.findChild("manual-action-actions-spacer").isPresent());
        assertEquals(
                List.of(new TaskViewEvent("Aufgabe abschließen", "complete")),
                node.getStaffTaskViewEvents(context)
        );

        var data = node.getStaffTaskViewData(context);
        assertEquals("Grace", data.get("applicantName"));
        assertEquals("<p>Entwurf gespeichert.</p>", data.get("manualActionRemark"));
    }

    @Test
    void getStaffTaskView_RendersConfiguredDescriptionAndUi() throws Exception {
        var processData = Map.<String, Object>of("applicant", Map.of("name", "Ada"));

        var context = new ProcessNodeExecutionContextUIStaff(
                logger(),
                processNode(configuration()),
                processInstance("process-owner"),
                task(
                        77,
                        Map.of(),
                        Map.of(),
                        processData
                ),
                null,
                user("staff-1"),
                nodeConfiguration(configuration()),
                currentProcessData(processData)
        );

        var layout = node.getStaffTaskView(context);
        var description = layout.findChild("manual-action-description-content", RichTextContentElement.class).orElseThrow();
        var dataField = layout.findChild("applicantName", TextInputElement.class).orElseThrow();
        var remarkField = layout.findChild("manualActionRemark", RichTextInputElement.class).orElseThrow();

        assertEquals("<p>Bitte führen Sie die Prüfung vor Ort durch.</p>", description.getContent());
        assertFalse(Boolean.TRUE.equals(dataField.getDisabled()));
        assertTrue(layout.findChild("manual-action-remark-spacer").isPresent());
        assertEquals(6.0, remarkField.getWeight());
        assertTrue(layout.findChild("manual-action-actions-spacer").isPresent());
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
                        authored(
                                "applicantName", "Grace",
                                "manualActionRemark", "<p>Entwurf gespeichert.</p>"
                        ),
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
                authored(
                        "applicantName", "Grace",
                        "manualActionRemark", "<p>Entwurf gespeichert.</p>"
                )
        );

        assertTrue(result.isPresent());

        var updated = assertInstanceOf(ProcessNodeExecutionResultTaskUpdated.class, result.get());
        assertEquals("value", updated.getRuntimeData().get("keep"));
        assertEquals(Map.of("existing", "node-data"), updated.getNodeData());
        assertEquals(Map.of("applicant", Map.of("name", "Ada")), updated.getProcessData());

        var draftData = updated.getRuntimeData().get(ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY);
        assertNotNull(draftData);
        assertEquals("Grace", ((Map<?, ?>) draftData).get("applicantName"));
        assertEquals("<p>Entwurf gespeichert.</p>", ((Map<?, ?>) draftData).get("manualActionRemark"));
    }

    @Test
    void onEventFromStaffTaskView_CompleteMergesProcessDataAndStoresRemarkAndDiff() throws Exception {
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
                                        authored(
                                                "applicantName", "Draft",
                                                "manualActionRemark", "<p>Vorab erfasst.</p>"
                                        )
                                ),
                                Map.of(),
                                processData
                        ),
                        null,
                        user("staff-1"),
                        nodeConfiguration(configuration()),
                        currentProcessData(processData)
                ),
                authored(
                        "applicantName", "Grace",
                        "manualActionRemark", "<p>Vor Ort durchgeführt.</p>"
                ),
                "complete"
        );

        assertTrue(result.isPresent());

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result.get());
        assertEquals("output", completed.getViaPort());
        assertEquals(Map.of(), completed.getRuntimeData());
        assertEquals("staff-1", completed.getNodeData().get("processedByUserId"));
        assertEquals("<p>Vor Ort durchgeführt.</p>", completed.getNodeData().get("remark"));
        assertNotNull(completed.getNodeData().get("processedAt"));

        @SuppressWarnings("unchecked")
        var applicant = (Map<String, Object>) completed.getProcessData().get("applicant");
        assertEquals("Grace", applicant.get("name"));
        assertEquals(33, applicant.get("age"));
        assertEquals("value", completed.getProcessData().get("untouched"));

        @SuppressWarnings("unchecked")
        var changedData = (Map<String, Object>) completed.getNodeData().get("data");
        assertEquals(Map.of("name", "Grace"), changedData.get("applicant"));
        assertFalse(completed.getNodeData().containsKey("diff"));
    }

    @Test
    void onEventFromStaffTaskView_CompleteKeepsEquivalentTemporalValuesStable() throws Exception {
        var processData = Map.<String, Object>of(
                "date", "2026-05-09",
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
                        "dateField", "2026-05-09",
                        "dateTimeField", "2021-02-07T11:15:00.000Z"
                ),
                "complete"
        );

        assertTrue(result.isPresent());

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result.get());

        @SuppressWarnings("unchecked")
        var changedData = (Map<String, Object>) completed.getNodeData().get("data");
        assertEquals("2026-05-09", changedData.get("date"));
        assertEquals("2021-02-07T12:15:00+01:00", changedData.get("datetime"));
        assertFalse(completed.getNodeData().containsKey("diff"));
    }

    @Test
    void onEventFromStaffTaskView_CompletePreservesExistingNullProcessDataEntries() throws Exception {
        var firstArrayItem = new LinkedHashMap<String, Object>();
        firstArrayItem.put("alter", null);
        firstArrayItem.put("name", "Ada");

        var person = new LinkedHashMap<String, Object>();
        person.put("vorname", null);
        person.put("arrtest", List.of(firstArrayItem));

        var processData = new LinkedHashMap<String, Object>();
        processData.put("key", null);
        processData.put("person", person);
        processData.put("dynamischerDatenschluessel", "vorher");

        var configuration = configuration("dynamischerDatenschluessel");

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
                authored("applicantName", "reset"),
                "complete"
        );

        assertTrue(result.isPresent());

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result.get());
        assertEquals("reset", completed.getProcessData().get("dynamischerDatenschluessel"));
        assertTrue(completed.getProcessData().containsKey("key"));
        assertNull(completed.getProcessData().get("key"));

        @SuppressWarnings("unchecked")
        var completedPerson = (Map<String, Object>) completed.getProcessData().get("person");
        assertTrue(completedPerson.containsKey("vorname"));
        assertNull(completedPerson.get("vorname"));

        @SuppressWarnings("unchecked")
        var arrtest = (List<Map<String, Object>>) completedPerson.get("arrtest");
        assertTrue(arrtest.getFirst().containsKey("alter"));
        assertNull(arrtest.getFirst().get("alter"));
        assertEquals("Ada", arrtest.getFirst().get("name"));
    }

    @Test
    void onEventFromStaffTaskView_WithoutUiDefinitionCompletesWithoutDataChanges() throws Exception {
        var processData = Map.<String, Object>of("status", "open");

        var result = node.onEventFromStaffTaskView(
                new ProcessNodeExecutionContextUIStaff(
                        logger(),
                        processNode(configurationWithoutUi()),
                        processInstance("process-owner"),
                        task(
                                77,
                                Map.of(),
                                Map.of(),
                                processData
                        ),
                        null,
                        user("staff-1"),
                        nodeConfiguration(configurationWithoutUi()),
                        currentProcessData(processData)
                ),
                authored("manualActionRemark", "<p>Telefonisch bestätigt.</p>"),
                "complete"
        );

        assertTrue(result.isPresent());

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result.get());
        assertEquals(Map.of("status", "open"), completed.getProcessData());
        assertEquals(Map.of(), completed.getNodeData().get("data"));
        assertFalse(completed.getNodeData().containsKey("diff"));
        assertEquals("<p>Telefonisch bestätigt.</p>", completed.getNodeData().get("remark"));
    }

    private static AuthoredElementValues configuration() {
        return configuration("applicant.name");
    }

    private static AuthoredElementValues configuration(String destinationKey) {
        var contentRoot = new GroupLayoutElement();
        contentRoot.setId("manual-action-root");

        var valueField = new TextInputElement();
        valueField.setId("applicantName");
        valueField.setLabel("Name");
        valueField.setDestinationKey(destinationKey);
        contentRoot.setChildren(List.of(valueField));

        return authored(
                "task_description", "<p>Bitte führen Sie die Prüfung vor Ort durch.</p>",
                "ui_definition", contentRoot,
                "assignment_context", assignmentContext()
        );
    }

    private static AuthoredElementValues configurationWithTemporalFields() {
        var contentRoot = new GroupLayoutElement();
        contentRoot.setId("manual-action-root");

        var dateField = new DateInputElement();
        dateField.setId("dateField");
        dateField.setLabel("Datum");
        dateField.setDestinationKey("date");

        var dateTimeField = new DateTimeInputElement();
        dateTimeField.setId("dateTimeField");
        dateTimeField.setLabel("Datum und Uhrzeit");
        dateTimeField.setDestinationKey("datetime");

        contentRoot.setChildren(List.of(dateField, dateTimeField));

        return authored(
                "task_description", "<p>Bitte prüfen Sie die Zeitwerte.</p>",
                "ui_definition", contentRoot,
                "assignment_context", assignmentContext()
        );
    }

    private static AuthoredElementValues configurationWithoutUi() {
        return authored(
                "task_description", "<p>Bitte holen Sie die telefonische Bestätigung ein.</p>",
                "assignment_context", assignmentContext()
        );
    }

    private static AssignmentContextInputElementValue assignmentContext() {
        return new AssignmentContextInputElementValue()
                .setDomainAndUserSelection(List.of(new DomainAndUserSelectInputElementValue("user", "user-1")));
    }

    private static ElementDerivationService derivationService() {
        return new ElementDerivationService(
                new JavascriptEngineFactoryService(List.of()),
                new NoCodeEvaluationService(List.of()),
                new ElementDataTransformService(),
                new CodeListElementOptionsService(null, null)
        );
    }

    private static ProcessNodeEntity processNode(AuthoredElementValues configuration) {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("Manuelle Aktion ausführen")
                .setDataKey("manualActionNode")
                .setProcessNodeDefinitionKey("de.aivot.core.manual_action")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(configuration)
                .setOutputMappings(Map.of());
    }

    private static ManualActionNodeV1.ManualActionNodeConfig nodeConfiguration(AuthoredElementValues configuration)
            throws ElementDataConversionException {
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.putAll(configuration);
        return ElementPOJOMapper.mapToPOJO(effectiveValues, ManualActionNodeV1.ManualActionNodeConfig.class);
    }

    private static ProcessExecutionData currentProcessData(Map<String, Object> processData) {
        return new ProcessExecutionData().addProcessData(processData);
    }

    private static ProcessInstanceEntity processInstance(String assignedUserId) {
        var now = Instant.now();

        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setAccessKey(UUID.randomUUID().toString())
                .setProcessId(PROCESS_ID)
                .setInitialProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessInstanceStatus.Running)
                .setAssignedUserId(assignedUserId)
                .setAssignedFileNumbers(List.of())
                .setIdentities(new IdentityDataMap())
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
                .setAccessKey(UUID.randomUUID().toString())
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
        private Integer processId;
        private Integer processVersion;
        private Long processInstanceId;
        private Integer previousProcessNodeId;
        private String processInstanceAssignedUserId;
        private AssignmentContextInputElementValue assignmentContext;
        private List<String> requiredPermissions;

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
                                                Integer currentProcessNodeId,
                                                Long currentProcessInstanceTaskId,
                                                Integer previousProcessNodeId,
                                                String processInstanceAssignedUserId,
                                                AssignmentContextInputElementValue assignmentContext,
                                                List<String> requiredPermissions) {
            this.processId = processId;
            this.processVersion = processVersion;
            this.processInstanceId = processInstanceId;
            this.previousProcessNodeId = previousProcessNodeId;
            this.processInstanceAssignedUserId = processInstanceAssignedUserId;
            this.assignmentContext = assignmentContext;
            this.requiredPermissions = requiredPermissions;
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
