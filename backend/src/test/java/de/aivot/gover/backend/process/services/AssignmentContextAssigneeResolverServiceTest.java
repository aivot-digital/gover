package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.gover.backend.elements.models.elements.form.input.DomainAndUserSelectInputElementValue;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.gover.backend.process.repositories.VPotentialProcessInstanceAccessRepository;
import de.aivot.gover.backend.process.services.AssignmentContextAssigneeResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssignmentContextAssigneeResolverServiceTest {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Integer CURRENT_NODE_ID = 88;
    private static final Long CURRENT_TASK_ID = 123L;
    private static final Integer PREVIOUS_NODE_ID = 77;
    private static final String REQUIRED_PERMISSION = "process_instance.edit_task";

    private List<Object[]> accessRows;
    private ProcessInstanceTaskEntity previousTask;
    private ProcessInstanceTaskEntity previousIterationTask;
    private List<ProcessInstanceTaskEntity> processInstanceTasks;
    private List<ProcessInstanceTaskEntity> activeTasks;
    private Long capturedExcludedTaskId;
    private AssignmentContextAssigneeResolverService service;

    @BeforeEach
    void setUp() {
        accessRows = List.of();
        previousTask = null;
        previousIterationTask = null;
        processInstanceTasks = List.of();
        activeTasks = List.of();
        capturedExcludedTaskId = null;

        service = new AssignmentContextAssigneeResolverService(
                createPotentialAccessRepository(),
                createProcessInstanceTaskRepository()
        );
    }

    @Test
    void resolveAssignee_ChoosesLeastLoadedDirectCandidate() {
        accessRows = List.of(
                userRow("user-1", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("user-2", null, 20, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("deputy-user", 10, null, false, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        activeTasks = List.of(
                activeTask("user-1"),
                activeTask("user-1"),
                activeTask("user-2")
        );

        var result = service.resolveAssignee(
                PROCESS_ID,
                PROCESS_VERSION,
                PROCESS_INSTANCE_ID,
                CURRENT_NODE_ID,
                CURRENT_TASK_ID,
                null,
                null,
                assignmentContext(List.of(orgUnit("10"), team("20"))),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("user-2"), result);
    }

    @Test
    void resolveAssignee_PrefersPreviousTaskAssigneeWhenConfigured() {
        accessRows = List.of(
                userRow("user-1", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("user-2", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        previousTask = task("user-1");
        activeTasks = List.of(
                activeTask("user-1"),
                activeTask("user-1"),
                activeTask("user-2")
        );

        var result = service.resolveAssignee(
                PROCESS_ID,
                PROCESS_VERSION,
                PROCESS_INSTANCE_ID,
                CURRENT_NODE_ID,
                CURRENT_TASK_ID,
                PREVIOUS_NODE_ID,
                null,
                assignmentContext(List.of(orgUnit("10")))
                        .setGeneralAssigneePreference(AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("user-1"), result);
    }

    @Test
    void resolveAssignee_PrefersProcessInstanceAssigneeWhenConfigured() {
        accessRows = List.of(
                userRow("user-1", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("user-2", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        activeTasks = List.of(activeTask("user-1"));

        var result = service.resolveAssignee(
                PROCESS_ID,
                PROCESS_VERSION,
                PROCESS_INSTANCE_ID,
                CURRENT_NODE_ID,
                CURRENT_TASK_ID,
                null,
                "user-2",
                assignmentContext(List.of(orgUnit("10")))
                        .setGeneralAssigneePreference(AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PROCESS_INSTANCE_ASSIGNEE),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("user-2"), result);
    }

    @Test
    void resolveAssignee_PrefersUninvolvedUserBeforeLoadBalancing() {
        accessRows = List.of(
                userRow("user-1", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("user-2", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        processInstanceTasks = List.of(task("user-1"));
        activeTasks = List.of(
                activeTask("user-2"),
                activeTask("user-2"),
                activeTask("user-2")
        );

        var result = service.resolveAssignee(
                PROCESS_ID,
                PROCESS_VERSION,
                PROCESS_INSTANCE_ID,
                CURRENT_NODE_ID,
                CURRENT_TASK_ID,
                null,
                null,
                assignmentContext(List.of(orgUnit("10")))
                        .setGeneralAssigneePreference(AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_UNINVOLVED_USER),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("user-2"), result);
    }

    @Test
    void resolveAssignee_PrefersPreviousIterationAssigneeWhenConfigured() {
        accessRows = List.of(
                userRow("user-1", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("user-2", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        previousIterationTask = task("user-1");
        activeTasks = List.of(
                activeTask("user-1"),
                activeTask("user-1"),
                activeTask("user-2")
        );

        var result = service.resolveAssignee(
                PROCESS_ID,
                PROCESS_VERSION,
                PROCESS_INSTANCE_ID,
                CURRENT_NODE_ID,
                CURRENT_TASK_ID,
                PREVIOUS_NODE_ID,
                null,
                assignmentContext(List.of(orgUnit("10")))
                        .setRepeatExecutionAssigneePreference(AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("user-1"), result);
        assertEquals(CURRENT_TASK_ID, capturedExcludedTaskId);
    }

    @Test
    void resolveAssignee_IgnoresPreviousIterationAssigneeOutsideCandidates() {
        accessRows = List.of(
                userRow("user-1", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("user-2", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        previousIterationTask = task("user-3");
        activeTasks = List.of(activeTask("user-1"));

        var result = service.resolveAssignee(
                PROCESS_ID,
                PROCESS_VERSION,
                PROCESS_INSTANCE_ID,
                CURRENT_NODE_ID,
                CURRENT_TASK_ID,
                PREVIOUS_NODE_ID,
                null,
                assignmentContext(List.of(orgUnit("10")))
                        .setRepeatExecutionAssigneePreference(AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("user-2"), result);
    }

    @Test
    void resolveAssignee_PrefersDifferentPreviousIterationAssigneeWhenConfigured() {
        accessRows = List.of(
                userRow("user-1", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("user-2", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("user-3", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        previousIterationTask = task("user-1");
        activeTasks = List.of(
                activeTask("user-2"),
                activeTask("user-3"),
                activeTask("user-3")
        );

        var result = service.resolveAssignee(
                PROCESS_ID,
                PROCESS_VERSION,
                PROCESS_INSTANCE_ID,
                CURRENT_NODE_ID,
                CURRENT_TASK_ID,
                PREVIOUS_NODE_ID,
                null,
                assignmentContext(List.of(orgUnit("10")))
                        .setRepeatExecutionAssigneePreference(AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_DIFFERENT_FROM_PREVIOUS_ITERATION_ASSIGNEE),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("user-2"), result);
        assertEquals(CURRENT_TASK_ID, capturedExcludedTaskId);
    }

    @Test
    void resolveAssignee_FallsBackWhenDifferentPreviousIterationAssigneeIsOnlyCandidate() {
        accessRows = List.<Object[]>of(
                userRow("user-1", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        previousIterationTask = task("user-1");

        var result = service.resolveAssignee(
                PROCESS_ID,
                PROCESS_VERSION,
                PROCESS_INSTANCE_ID,
                CURRENT_NODE_ID,
                CURRENT_TASK_ID,
                PREVIOUS_NODE_ID,
                null,
                assignmentContext(List.of(orgUnit("10")))
                        .setRepeatExecutionAssigneePreference(AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_DIFFERENT_FROM_PREVIOUS_ITERATION_ASSIGNEE),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("user-1"), result);
    }

    @Test
    void resolveAssignee_AppliesGeneralPreferenceAfterDifferentPreviousIterationAssignee() {
        accessRows = List.of(
                userRow("user-1", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("user-2", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("user-3", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        previousIterationTask = task("user-1");
        previousTask = task("user-2");
        activeTasks = List.of(
                activeTask("user-2"),
                activeTask("user-2"),
                activeTask("user-3")
        );

        var result = service.resolveAssignee(
                PROCESS_ID,
                PROCESS_VERSION,
                PROCESS_INSTANCE_ID,
                CURRENT_NODE_ID,
                CURRENT_TASK_ID,
                PREVIOUS_NODE_ID,
                null,
                assignmentContext(List.of(orgUnit("10")))
                        .setRepeatExecutionAssigneePreference(AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_DIFFERENT_FROM_PREVIOUS_ITERATION_ASSIGNEE)
                        .setGeneralAssigneePreference(AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("user-2"), result);
    }

    @Test
    void resolveAssignee_PrefersPreviousIterationBeforePreviousProcessStep() {
        accessRows = List.of(
                userRow("user-1", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("user-2", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        previousTask = task("user-1");
        previousIterationTask = task("user-2");

        var result = service.resolveAssignee(
                PROCESS_ID,
                PROCESS_VERSION,
                PROCESS_INSTANCE_ID,
                CURRENT_NODE_ID,
                CURRENT_TASK_ID,
                PREVIOUS_NODE_ID,
                null,
                assignmentContext(List.of(orgUnit("10")))
                        .setRepeatExecutionAssigneePreference(AssignmentContextInputElementValue.REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE)
                        .setGeneralAssigneePreference(AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("user-2"), result);
    }

    private static AssignmentContextInputElementValue assignmentContext(List<DomainAndUserSelectInputElementValue> selection) {
        return new AssignmentContextInputElementValue()
                .setDomainAndUserSelection(selection);
    }

    private static DomainAndUserSelectInputElementValue orgUnit(String id) {
        return new DomainAndUserSelectInputElementValue("orgUnit", id);
    }

    private static DomainAndUserSelectInputElementValue team(String id) {
        return new DomainAndUserSelectInputElementValue("team", id);
    }

    private static ProcessInstanceTaskEntity task(String assignedUserId) {
        return new ProcessInstanceTaskEntity()
                .setPreviousProcessInstanceTaskId(null)
                .setPreviousProcessNodePortKey(null)
                .setAssignedUserId(assignedUserId);
    }

    private static ProcessInstanceTaskEntity activeTask(String assignedUserId) {
        return task(assignedUserId)
                .setStatus(ProcessTaskStatus.Running);
    }

    private static Object[] userRow(
            String userId,
            Integer viaDepartmentId,
            Integer viaTeamId,
            boolean isDirectMember,
            List<String> directPermissions,
            List<String> permissions
    ) {
        return new Object[]{
                null,
                null,
                userId,
                true,
                viaDepartmentId,
                viaTeamId,
                isDirectMember,
                directPermissions.toArray(String[]::new),
                permissions.toArray(String[]::new)
        };
    }

    private VPotentialProcessInstanceAccessRepository createPotentialAccessRepository() {
        return proxy(VPotentialProcessInstanceAccessRepository.class, (methodName, args) -> switch (methodName) {
            case "findRowsByProcessIdAndProcessVersion" -> accessRows;
            default -> unsupported(methodName);
        });
    }

    private ProcessInstanceTaskRepository createProcessInstanceTaskRepository() {
        return proxy(ProcessInstanceTaskRepository.class, (methodName, args) -> switch (methodName) {
            case "findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc" -> Optional.ofNullable(previousTask);
            case "findFirstByProcessInstanceIdAndProcessNodeIdAndIdNotOrderByStartedDesc" -> {
                capturedExcludedTaskId = (Long) args[2];
                yield Optional.ofNullable(previousIterationTask);
            }
            case "findAllByProcessInstanceId" -> processInstanceTasks;
            case "findAllByAssignedUserIdInAndStatusIn" -> activeTasks;
            default -> unsupported(methodName);
        });
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
