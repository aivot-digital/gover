package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.GoverBackend.elements.models.elements.form.input.DomainAndUserSelectInputElementValue;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.models.lib.DiffItem;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextInit;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextUIStaff;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResultTaskCompleted;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static de.aivot.GoverBackend.TestData.authored;
import static de.aivot.GoverBackend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        node = new DataChangeActionNodeV1(assigneeResolverService, new ElementDataTransformService());
    }

    @Test
    void init_AssignsResolvedUserAndCopiesWorkingProcessData() throws Exception {
        assigneeResolverService.result = Optional.of("user-1");

        var processData = new ProcessExecutionData()
                .addProcessData(Map.of(
                        "applicant", Map.of("name", "Ada"),
                        "meta", Map.of("source", "task")
                ));

        var result = node.init(new ProcessNodeExecutionContextInit(
                logger(),
                processNode(configuration()),
                processInstance("process-owner"),
                task(77, Map.of(), Map.of(), Map.of("applicant", Map.of("name", "Ada"))),
                null,
                processData,
                runtime(configuration())
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
        var context = new ProcessNodeExecutionContextUIStaff(
                logger(),
                processNode(configuration()),
                processInstance("process-owner"),
                task(
                        77,
                        Map.of("draftData", authored("applicantName", "Grace")),
                        Map.of(),
                        Map.of("applicant", Map.of("name", "Ada"))
                ),
                null,
                user("staff-1"),
                runtime(configuration()),
                null
        );

        var layout = node.getStaffTaskView(context);
        var dataField = layout.findChild("applicantName", TextInputElement.class).orElseThrow();

        assertFalse(Boolean.TRUE.equals(dataField.getDisabled()));
        assertEquals(
                List.of(
                        new TaskViewEvent("Speichern", "save"),
                        new TaskViewEvent("Speichern und abschließen", "complete", "outlined", null, "right")
                ),
                node.getStaffTaskViewEvents(context)
        );

        var data = node.getStaffTaskViewData(context);
        assertEquals("Grace", data.get("applicantName"));
    }

    @Test
    void getStaffTaskView_RendersConfiguredTaskDescription() throws Exception {
        var context = new ProcessNodeExecutionContextUIStaff(
                logger(),
                processNode(configuration("<p>Bitte prüfen und korrigieren Sie die Personendaten.</p>")),
                processInstance("process-owner"),
                task(
                        77,
                        Map.of(),
                        Map.of(),
                        Map.of("applicant", Map.of("name", "Ada"))
                ),
                null,
                user("staff-1"),
                runtime(configuration("<p>Bitte prüfen und korrigieren Sie die Personendaten.</p>")),
                null
        );

        var layout = node.getStaffTaskView(context);
        var description = layout.findChild("data-change-task-view-description-content", RichTextContentElement.class).orElseThrow();

        assertEquals("<p>Bitte prüfen und korrigieren Sie die Personendaten.</p>", description.getContent());
    }

    @Test
    void onUpdateFromStaff_SaveKeepsTaskRunningAndPersistsDraft() throws Exception {
        var result = node.onUpdateFromStaff(
                new ProcessNodeExecutionContextUIStaff(
                        logger(),
                        processNode(configuration()),
                        processInstance("process-owner"),
                        task(
                                77,
                                Map.of(),
                                Map.of("existing", "node-data"),
                                Map.of("applicant", Map.of("name", "Ada"))
                        ),
                        null,
                        user("staff-1"),
                        runtime(configuration()),
                        null
                ),
                authored("applicantName", "Grace"),
                "save"
        );

        assertTrue(result.isPresent());

        var assigned = assertInstanceOf(ProcessNodeExecutionResultTaskAssigned.class, result.get());
        assertEquals("staff-1", assigned.getAssignedUserId());
        assertEquals(Map.of("applicant", Map.of("name", "Ada")), assigned.getProcessData());
        assertEquals(Map.of("existing", "node-data"), assigned.getNodeData());

        var draftData = assigned.getRuntimeData().get("draftData");
        assertNotNull(draftData);
        assertEquals("Grace", ((Map<?, ?>) draftData).get("applicantName"));
    }

    @Test
    void onUpdateFromStaff_CompleteMergesProcessDataAndStoresDiff() throws Exception {
        var result = node.onUpdateFromStaff(
                new ProcessNodeExecutionContextUIStaff(
                        logger(),
                        processNode(configuration()),
                        processInstance("process-owner"),
                        task(
                                77,
                                Map.of("draftData", authored("applicantName", "Draft")),
                                Map.of(),
                                Map.of(
                                        "applicant", Map.of("name", "Ada", "age", 33),
                                        "untouched", "value"
                                )
                        ),
                        null,
                        user("staff-1"),
                        runtime(configuration()),
                        null
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
        assertEquals("/applicant/name", diff.getFirst().field());
        assertEquals("Ada", diff.getFirst().oldValue());
        assertEquals("Grace", diff.getFirst().newValue());
    }

    private static AuthoredElementValues configuration() {
        return configuration(null);
    }

    private static AuthoredElementValues configuration(String taskDescription) {
        var contentRoot = new GroupLayoutElement();
        contentRoot.setId("data-change-root");

        var valueField = new TextInputElement();
        valueField.setId("applicantName");
        valueField.setLabel("Name");
        valueField.setDestinationKey("applicant.name");
        contentRoot.setChildren(List.of(valueField));

        var configuration = new AuthoredElementValues();
        configuration.put("data_definition", contentRoot);
        configuration.put("assignment_context", assignmentContext());
        if (taskDescription != null) {
            configuration.put("task_description", taskDescription);
        }

        return configuration;
    }

    private static AssignmentContextInputElementValue assignmentContext() {
        return new AssignmentContextInputElementValue()
                .setDomainAndUserSelection(List.of(new DomainAndUserSelectInputElementValue("user", "user-1")))
                .setPreferPreviousTaskAssignee(false)
                .setPreferUninvolvedUser(false)
                .setPreferProcessInstanceAssignee(false);
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

    private static ProcessInstanceEntity processInstance(String assignedUserId) {
        var now = LocalDateTime.now();

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
        var now = LocalDateTime.now();

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
