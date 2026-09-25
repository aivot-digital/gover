package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DomainAndUserSelectInputElementValue;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.VPotentialProcessInstanceAccessRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
import static org.mockito.Mockito.*;

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
    private PermissionService permissions;

    @BeforeEach
    void setUp() {
        accessRows = List.of();
        previousTask = null;
        previousIterationTask = null;
        processInstanceTasks = List.of();
        activeTasks = List.of();
        capturedExcludedTaskId = null;

        permissions = mock(PermissionService.class);
        when(permissions.hasProcessInstancePermissionWithoutDeputies(anyString(), eq(PROCESS_INSTANCE_ID), anyString()))
                .thenReturn(true);
        var users = mock(UserRepository.class);
        when(users.findById(anyString())).thenAnswer(invocation -> Optional.of(new UserEntity()
                .setId(invocation.getArgument(0)).setEnabled(true).setDeletedInIdp(false)));
        var tasks = createProcessInstanceTaskRepository();
        var assignments = new ProcessAssignmentService(permissions, users, mock(ProcessInstanceRepository.class),
                tasks, mock(AuditService.class));
        service = new AssignmentContextAssigneeResolverService(
                createPotentialAccessRepository(),
                tasks,
                assignments
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
    void resolveAssignee_AllowsExplicitUserWithCurrentSystemOrInstanceAccessWithoutViewRow() {
        var result = service.resolveAssignee(
                PROCESS_ID,
                PROCESS_VERSION,
                PROCESS_INSTANCE_ID,
                CURRENT_NODE_ID,
                CURRENT_TASK_ID,
                null,
                null,
                assignmentContext(List.of(new DomainAndUserSelectInputElementValue("user", "globally-authorized"))),
                List.of("process_instance.read"),
                Set.of("globally-authorized")
        );

        assertEquals(Optional.of("globally-authorized"), result);
    }

    @Test
    void resolveAssignee_IgnoresCandidateWithoutRequiredProcessAccess() {
        accessRows = List.of(
                userRow("user-1", null, 20, true, List.of(REQUIRED_PERMISSION), List.of()),
                userRow("user-2", null, 20, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );

        var result = service.resolveAssignee(
                PROCESS_ID,
                PROCESS_VERSION,
                PROCESS_INSTANCE_ID,
                CURRENT_NODE_ID,
                CURRENT_TASK_ID,
                null,
                null,
                assignmentContext(List.of(team("20"))),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("user-2"), result);
    }

    @Test
    void resolveAssignee_IgnoresDirectMembersWithOnlyDeputyPermission() {
        accessRows = List.of(
                userRow("deputy", null, 20, true, List.of("process_instance.read"), List.of(REQUIRED_PERMISSION)),
                userRow("own-access", null, 20, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        activeTasks = List.of(activeTask("own-access"));

        var result = service.resolveAssignee(
                PROCESS_ID, PROCESS_VERSION, PROCESS_INSTANCE_ID, CURRENT_NODE_ID, CURRENT_TASK_ID,
                null, null, assignmentContext(List.of(team("20"))), List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("own-access"), result);
    }

    @Test
    void resolveAssignee_SkipsPreferredAndLessLoadedCandidateWithoutReadPermission() {
        accessRows = List.of(
                userRow("user-1", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("user-2", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        when(permissions.hasProcessInstancePermissionWithoutDeputies("user-1", PROCESS_INSTANCE_ID, PROCESS_INSTANCE_READ))
                .thenReturn(false);
        previousTask = task("user-1");
        activeTasks = List.of(activeTask("user-2"));

        var result = service.resolveAssignee(
                PROCESS_ID, PROCESS_VERSION, PROCESS_INSTANCE_ID, CURRENT_NODE_ID, CURRENT_TASK_ID,
                PREVIOUS_NODE_ID, null, assignmentContext(List.of(orgUnit("10")))
                        .setGeneralAssigneePreference(AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("user-2"), result);
    }

    @Test
    void resolveAssignee_ExcludesPreviouslyEligibleUserAfterInstancePermissionWithdrawal() {
        accessRows = List.of(
                userRow("withdrawn", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("granted", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        when(permissions.hasProcessInstancePermissionWithoutDeputies("withdrawn", PROCESS_INSTANCE_ID, REQUIRED_PERMISSION))
                .thenReturn(false);
        activeTasks = List.of(activeTask("granted"));

        var result = service.resolveAssignee(
                PROCESS_ID, PROCESS_VERSION, PROCESS_INSTANCE_ID, CURRENT_NODE_ID, CURRENT_TASK_ID,
                null, null, assignmentContext(List.of(orgUnit("10"))), List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("granted"), result);
    }

    @Test
    void resolveAssignee_DoesNotExpandExplicitSelectionBeyondTheConfiguredPool() {
        accessRows = List.<Object[]>of(
                userRow("candidate", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        var result = service.resolveAssignee(
                PROCESS_ID, PROCESS_VERSION, PROCESS_INSTANCE_ID, CURRENT_NODE_ID, CURRENT_TASK_ID,
                null, null, assignmentContext(List.of(user("global"))), List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.empty(), result);
        verifyNoInteractions(permissions);
    }

    @ParameterizedTest
    @ValueSource(strings = {"orgUnit", "team"})
    void resolveAssignee_DoesNotUseAnotherMembershipToQualifyForTheSelectedGroup(String selectionType) {
        var department = "orgUnit".equals(selectionType);
        accessRows = List.of(
                userRow("cross-member", department ? 10 : null, department ? null : 10, true,
                        List.of(PROCESS_INSTANCE_READ), List.of(PROCESS_INSTANCE_READ)),
                userRow("cross-member", department ? 20 : null, department ? null : 20, true,
                        List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("eligible-member", department ? 10 : null, department ? null : 10, true,
                        List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        // Both have own instance rights, but only eligible-member qualifies through the selected group.
        previousTask = task("cross-member");
        activeTasks = List.of(activeTask("eligible-member"));

        var result = service.resolveAssignee(
                PROCESS_ID, PROCESS_VERSION, PROCESS_INSTANCE_ID, CURRENT_NODE_ID, CURRENT_TASK_ID,
                PREVIOUS_NODE_ID, null,
                assignmentContext(List.of(new DomainAndUserSelectInputElementValue(selectionType, "10")))
                        .setGeneralAssigneePreference(AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("eligible-member"), result);
        verify(permissions, never()).hasProcessInstancePermissionWithoutDeputies(eq("cross-member"), anyLong(), anyString());
    }

    @Test
    void resolveAssignee_DoesNotFallBackOutsideTheSelectionWhenNoEligibleCandidateRemains() {
        accessRows = List.of(
                userRow("selected", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("outside", 20, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION))
        );
        when(permissions.hasProcessInstancePermissionWithoutDeputies("selected", PROCESS_INSTANCE_ID, PROCESS_INSTANCE_READ))
                .thenReturn(false);

        var result = service.resolveAssignee(
                PROCESS_ID, PROCESS_VERSION, PROCESS_INSTANCE_ID, CURRENT_NODE_ID, CURRENT_TASK_ID,
                null, "outside", assignmentContext(List.of(orgUnit("10")))
                        .setGeneralAssigneePreference(AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PROCESS_INSTANCE_ASSIGNEE),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.empty(), result);
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
    void resolveAssignee_FallsBackWhenProcessInstanceHasNoAssignee() {
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
                null,
                assignmentContext(List.of(orgUnit("10")))
                        .setGeneralAssigneePreference(AssignmentContextInputElementValue.GENERAL_ASSIGNEE_PREFERENCE_PROCESS_INSTANCE_ASSIGNEE),
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(Optional.of("user-2"), result);
    }

    @Test
    void resolveAssignee_FallsBackWhenProcessInstanceAssigneeIsOutsideSelection() {
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
                "user-3",
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

    private static DomainAndUserSelectInputElementValue user(String id) {
        return new DomainAndUserSelectInputElementValue("user", id);
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
