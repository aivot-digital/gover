package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.GoverBackend.elements.models.elements.form.input.DomainAndUserSelectInputElementValue;
import de.aivot.GoverBackend.elements.models.elements.form.input.RichTextInputElement;
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
        node = new ManualActionNodeV1(assigneeResolverService, new ElementDataTransformService());
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
        assertEquals(PROCESS_ID, assigneeResolverService.processId);
        assertEquals(PROCESS_VERSION, assigneeResolverService.processVersion);
        assertEquals(PROCESS_INSTANCE_ID, assigneeResolverService.processInstanceId);
        assertEquals(77, assigneeResolverService.previousProcessNodeId);
        assertEquals("process-owner", assigneeResolverService.processInstanceAssignedUserId);
        assertEquals(assignmentContext(), assigneeResolverService.assignmentContext);
        assertEquals(List.of("process_instance.edit_task"), assigneeResolverService.requiredPermissions);
    }

    @Test
    void getStaffTaskView_RendersConfiguredDescriptionAndUi() throws Exception {
        var context = new ProcessNodeExecutionContextUIStaff(
                logger(),
                processNode(configuration()),
                processInstance("process-owner"),
                task(
                        77,
                        Map.of(),
                        Map.of(),
                        Map.of("applicant", Map.of("name", "Ada"))
                ),
                null,
                user("staff-1"),
                runtime(configuration())
        );

        var layout = node.getStaffTaskView(context);
        var description = layout.findChild("manual-action-description-content", RichTextContentElement.class).orElseThrow();
        var dataField = layout.findChild("applicantName", TextInputElement.class).orElseThrow();
        var remarkField = layout.findChild("manualActionRemark", RichTextInputElement.class).orElseThrow();

        assertEquals("<p>Bitte führen Sie die Prüfung vor Ort durch.</p>", description.getContent());
        assertFalse(Boolean.TRUE.equals(dataField.getDisabled()));
        assertTrue(layout.findChild("manual-action-remark-spacer").isPresent());
        assertTrue(Boolean.TRUE.equals(remarkField.getReducedMode()));
        assertEquals(6.0, remarkField.getWeight());
        assertTrue(layout.findChild("manual-action-actions-spacer").isPresent());
        assertEquals(
                List.of(new TaskViewEvent("Aufgabe bestätigen", "confirm")),
                node.getStaffTaskViewEvents(context)
        );

        var data = node.getStaffTaskViewData(context);
        assertEquals("Ada", data.get("applicantName"));
    }

    @Test
    void onUpdateFromStaff_ConfirmMergesProcessDataAndStoresRemarkAndDiff() throws Exception {
        var result = node.onUpdateFromStaff(
                new ProcessNodeExecutionContextUIStaff(
                        logger(),
                        processNode(configuration()),
                        processInstance("process-owner"),
                        task(
                                77,
                                Map.of(),
                                Map.of(),
                                Map.of(
                                        "applicant", Map.of("name", "Ada", "age", 33),
                                        "untouched", "value"
                                )
                        ),
                        null,
                        user("staff-1"),
                        runtime(configuration())
                ),
                authored(
                        "applicantName", "Grace",
                        "manualActionRemark", "<p>Vor Ort durchgeführt.</p>"
                ),
                "confirm"
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

        @SuppressWarnings("unchecked")
        var diff = (List<DiffItem>) completed.getNodeData().get("diff");
        assertEquals(1, diff.size());
        assertEquals("/applicant/name", diff.getFirst().field());
        assertEquals("Ada", diff.getFirst().oldValue());
        assertEquals("Grace", diff.getFirst().newValue());
    }

    @Test
    void onUpdateFromStaff_WithoutUiDefinitionCompletesWithoutDataChanges() throws Exception {
        var result = node.onUpdateFromStaff(
                new ProcessNodeExecutionContextUIStaff(
                        logger(),
                        processNode(configurationWithoutUi()),
                        processInstance("process-owner"),
                        task(
                                77,
                                Map.of(),
                                Map.of(),
                                Map.of("status", "open")
                        ),
                        null,
                        user("staff-1"),
                        runtime(configurationWithoutUi())
                ),
                authored("manualActionRemark", "<p>Telefonisch bestätigt.</p>"),
                "confirm"
        );

        assertTrue(result.isPresent());

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result.get());
        assertEquals(Map.of("status", "open"), completed.getProcessData());
        assertEquals(Map.of(), completed.getNodeData().get("data"));
        assertEquals(List.of(), completed.getNodeData().get("diff"));
        assertEquals("<p>Telefonisch bestätigt.</p>", completed.getNodeData().get("remark"));
    }

    private static AuthoredElementValues configuration() {
        var contentRoot = new GroupLayoutElement();
        contentRoot.setId("manual-action-root");

        var valueField = new TextInputElement();
        valueField.setId("applicantName");
        valueField.setLabel("Name");
        valueField.setDestinationKey("applicant.name");
        contentRoot.setChildren(List.of(valueField));

        return authored(
                "task_description", "<p>Bitte führen Sie die Prüfung vor Ort durch.</p>",
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
                .setName("Manuelle Aktion ausführen")
                .setDataKey("manualActionNode")
                .setProcessNodeDefinitionKey("de.aivot.core.manual_action")
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
