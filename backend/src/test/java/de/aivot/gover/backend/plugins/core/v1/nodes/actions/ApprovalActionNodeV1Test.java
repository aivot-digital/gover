package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import de.aivot.gover.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.EffectiveElementValues;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.gover.backend.elements.services.ElementDerivationService;
import de.aivot.gover.backend.elements.utils.ElementPOJOMapper;
import de.aivot.gover.backend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.gover.backend.elements.models.elements.form.input.DomainAndUserSelectInputElementValue;
import de.aivot.gover.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.elements.services.CodeListElementOptionsService;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.gover.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.models.ProcessNodeDefinition;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.gover.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.TaskViewEvent;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static de.aivot.gover.backend.TestData.authored;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;

    private TestAssignmentContextAssigneeResolverService assigneeResolverService;
    private ApprovalActionNodeV1 node;

    @BeforeEach
    void setUp() {
        assigneeResolverService = new TestAssignmentContextAssigneeResolverService();
        node = new ApprovalActionNodeV1(
                assigneeResolverService,
                new ElementDataTransformService(),
                new PassthroughTemplateRenderService(),
                derivationService()
        );
    }

    @Test
    void init_AssignsResolvedUser() throws Exception {
        assigneeResolverService.result = Optional.of("user-1");

        var processData = new ProcessExecutionData()
                .addProcessData(Map.of("approvalValue", "Freizugebender Inhalt"))
                .addProcessMetadata(Map.of("instanceId", PROCESS_INSTANCE_ID));

        var result = node.init(new ProcessNodeExecutionInitContext(
                logger(),
                processNode(dataModeConfiguration()),
                processInstance("process-owner"),
                task(77, Map.of(), Map.of("approvalValue", "Freizugebender Inhalt")),
                null,
                processData,
                nodeConfiguration(dataModeConfiguration())
        ));

        var taskAssigned = assertInstanceOf(ProcessNodeExecutionResultTaskAssigned.class, result);
        assertEquals("user-1", taskAssigned.getAssignedUserId());
        assertEquals(Map.of("approvalValue", "Freizugebender Inhalt"), taskAssigned.getProcessData());
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
    void init_RejectsAssignmentContextWithoutSelection() {
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of("approvalValue", "Freizugebender Inhalt"));

        assertThrows(
                ProcessNodeExecutionExceptionInvalidAssignment.class,
                () -> node.init(new ProcessNodeExecutionInitContext(
                        logger(),
                        processNode(configurationWithPreferenceOnlyAssignmentContext()),
                        processInstance("process-owner"),
                        task(77, Map.of(), Map.of("approvalValue", "Freizugebender Inhalt")),
                        null,
                        processData,
                        nodeConfiguration(configurationWithPreferenceOnlyAssignmentContext())
                ))
        );
    }

    @Test
    void getStaffTaskViewData_RendersConfiguredDataSummaryUi() throws Exception {
        var processData = Map.<String, Object>of("approvalValue", "Freizugebender Inhalt");

        var context = new ProcessNodeExecutionContextUIStaff(
                logger(),
                processNode(dataModeConfiguration()),
                processInstance("process-owner"),
                task(77, Map.of("approvalRemark", "<p>Schon geprüft</p>"), processData),
                null,
                user("staff-1"),
                nodeConfiguration(dataModeConfiguration()),
                currentProcessData(processData)
        );

        var layout = node.getStaffTaskView(context);
        var dataSummary = layout.findChild("approval-data-root", GroupLayoutElement.class).orElseThrow();
        var remarkField = layout.findChild("approvalRemark", RichTextInputElement.class).orElseThrow();
        assertTrue(dataSummary.findChild("approvalValue", TextInputElement.class).isPresent());
        assertEquals(6.0, remarkField.getWeight());
        assertTrue(layout.findChild("approval-actions-spacer").isPresent());

        var data = node.getStaffTaskViewData(context);
        assertEquals("Freizugebender Inhalt", data.get("approvalValue"));
        assertNull(data.get("approvalRemark"));
        assertEquals(
                List.of(
                        new TaskViewEvent("Freigeben", "approve"),
                        new TaskViewEvent("Ablehnen", "reject")
                ),
                node.getStaffTaskViewEvents(context)
        );
    }

    @Test
    void getStaffTaskViewData_LoadsSavedDraftSnapshotFromRuntimeData() throws Exception {
        var processData = Map.<String, Object>of("approvalValue", "Freizugebender Inhalt");

        var context = new ProcessNodeExecutionContextUIStaff(
                logger(),
                processNode(dataModeConfiguration()),
                processInstance("process-owner"),
                task(
                        77,
                        Map.of(
                                ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY,
                                authored(
                                        "approvalValue", "Freizugebender Inhalt",
                                        "approvalRemark", "<p>Schon geprüft</p>"
                                )
                        ),
                        Map.of(),
                        processData
                ),
                null,
                user("staff-1"),
                nodeConfiguration(dataModeConfiguration()),
                currentProcessData(processData)
        );

        var data = node.getStaffTaskViewData(context);
        assertEquals("Freizugebender Inhalt", data.get("approvalValue"));
        assertEquals("<p>Schon geprüft</p>", data.get("approvalRemark"));
    }

    @Test
    void onEventFromStaffTaskView_CompletesTaskViaSelectedPort() throws Exception {
        var processData = Map.<String, Object>of("approvalValue", "Freizugebender Inhalt");

        var result = node.onEventFromStaffTaskView(
                new ProcessNodeExecutionContextUIStaff(
                        logger(),
                        processNode(dataModeConfiguration()),
                        processInstance("process-owner"),
                        task(77, Map.of(), processData),
                        null,
                        user("staff-1"),
                        nodeConfiguration(dataModeConfiguration()),
                        currentProcessData(processData)
                ),
                authored("approvalRemark", "<p>Passt</p>"),
                "approve"
        );

        assertTrue(result.isPresent());

        var completed = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, result.get());
        assertEquals("approved", completed.getViaPort());
        assertEquals("approved", completed.getNodeData().get("decision"));
        assertEquals("<p>Passt</p>", completed.getNodeData().get("remark"));
        assertEquals("staff-1", completed.getNodeData().get("processedByUserId"));
        assertInstanceOf(Instant.class, completed.getNodeData().get("processedAt"));
    }

    private static AuthoredElementValues dataModeConfiguration() {
        var contentRoot = new GroupLayoutElement();
        contentRoot.setId("approval-data-root");

        var valueField = new TextInputElement();
        valueField.setId("approvalValue");
        valueField.setLabel("Wert");
        valueField.setDestinationKey("approvalValue");
        contentRoot.setChildren(List.of(valueField));

        return authored(
                "criteria", "<p>Bitte fachlich prüfen.</p>",
                "contentMode", "data",
                "dataContent", contentRoot,
                "assignmentContext", assignmentContext()
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

    private static AuthoredElementValues configurationWithPreferenceOnlyAssignmentContext() {
        return authored(
                "criteria", "<p>Bitte fachlich prüfen.</p>",
                "contentMode", "custom",
                "customContent", "<p>Bitte in Drittsystem prüfen.</p>",
                "assignmentContext", new AssignmentContextInputElementValue()
                        .setDomainAndUserSelection(null)
                        .setGeneralAssigneePreference(AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE)
        );
    }

    private static ApprovalActionNodeV1.ApprovalConfiguration nodeConfiguration(AuthoredElementValues configuration)
            throws ElementDataConversionException {
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.putAll(configuration);
        return ElementPOJOMapper.mapToPOJO(effectiveValues, ApprovalActionNodeV1.ApprovalConfiguration.class);
    }

    private static ProcessExecutionData currentProcessData(Map<String, Object> processData) {
        return new ProcessExecutionData().addProcessData(processData);
    }

    private static ProcessNodeEntity processNode(AuthoredElementValues configuration) {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("Freigabe")
                .setDataKey("approvalNode")
                .setProcessNodeDefinitionKey("de.aivot.core.approval")
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(configuration)
                .setOutputMappings(Map.of());
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
            Map<String, Object> nodeData,
            Map<String, Object> processData
    ) {
        return task(previousProcessNodeId, Map.of(), nodeData, processData);
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
        public Optional<String> resolveAssignee(
                Integer processId,
                Integer processVersion,
                Long processInstanceId,
                Integer currentProcessNodeId,
                Long currentProcessInstanceTaskId,
                Integer previousProcessNodeId,
                String processInstanceAssignedUserId,
                AssignmentContextInputElementValue assignmentContext,
                List<String> requiredPermissions
        ) {
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

    private static class PassthroughTemplateRenderService extends TemplateRenderService {
        private PassthroughTemplateRenderService() {
            super(null);
        }

        @Override
        public String interpolate(ProcessExecutionData foldedProcessData, String template) {
            return template;
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
