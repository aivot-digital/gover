package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DomainAndUserSelectInputElementValue;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultInstanceAssigned;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.VPotentialProcessInstanceAccessRepository;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.ProcessAssignmentService;
import de.aivot.prosuna.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import de.aivot.prosuna.backend.user.services.UserDeputyService;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Runs the element with the actual resolver and assignment service; only repositories and permissions are mocked.
 */
class InstanceAssignmentActionNodeV1EligibilityTest {
    private static final long INSTANCE_ID = 99L;

    private final PermissionService permissions = mock(PermissionService.class);
    private final UserRepository users = mock(UserRepository.class);
    private InstanceAssignmentActionNodeV1 node;

    @BeforeEach
    void setUp() {
        var instances = mock(ProcessInstanceRepository.class);
        when(instances.existsById(INSTANCE_ID)).thenReturn(true);
        var tasks = mock(ProcessInstanceTaskRepository.class);
        var assignments = new ProcessAssignmentService(permissions, users, instances, tasks, mock(AuditService.class),
                mock(ProcessNodeExecutionLoggerFactory.class), mock(UserService.class),
                mock(AbstractConfigurableTemplateResolver.class));
        var resolver = new AssignmentContextAssigneeResolverService(
                mock(VPotentialProcessInstanceAccessRepository.class), tasks, assignments);
        node = new InstanceAssignmentActionNodeV1(resolver, assignments, mock(UserDeputyService.class));

        var reader = user("reader");
        var dispatcher = user("dispatcher");
        when(users.findAllByEnabledTrueAndDeletedInIdpFalseOrderByFullNameAsc()).thenReturn(List.of(reader, dispatcher));
        when(users.findById("reader")).thenReturn(Optional.of(reader));
        when(users.findById("dispatcher")).thenReturn(Optional.of(dispatcher));
        grantOwn("reader", PROCESS_INSTANCE_READ);
        grantOwn("dispatcher", PROCESS_INSTANCE_READ);
    }

    @Test
    void automaticModeAssignsThePersonWithOnlyOwnReadAccess() throws Exception {
        var result = assertInstanceOf(ProcessNodeExecutionResultInstanceAssigned.class,
                node.init(initContext(config("automatic", "reader"))));

        assertEquals("reader", result.getAssignedUserId());
    }

    @Test
    void manualModeStillRequiresOwnTaskPermissionsOfTheDispatcher() throws Exception {
        // Task editing and reassignment through a deputyship count for the dispatcher options, but not for the task.
        for (var permission : List.of(PROCESS_INSTANCE_EDIT_TASK, PROCESS_INSTANCE_REASSIGN)) {
            when(permissions.hasProcessInstancePermission("dispatcher", INSTANCE_ID, permission)).thenReturn(true);
        }
        assertThrows(ProcessNodeExecutionExceptionInvalidAssignment.class,
                () -> node.init(initContext(config("manual", "dispatcher"))));

        grantOwn("dispatcher", PROCESS_INSTANCE_EDIT_TASK);
        grantOwn("dispatcher", PROCESS_INSTANCE_REASSIGN);
        var result = assertInstanceOf(ProcessNodeExecutionResultTaskAssigned.class,
                node.init(initContext(config("manual", "dispatcher"))));
        assertEquals("dispatcher", result.getAssignedUserId());
    }

    private void grantOwn(String userId, String permission) {
        when(permissions.hasProcessInstancePermission(userId, INSTANCE_ID, permission)).thenReturn(true);
        when(permissions.hasProcessInstancePermissionWithoutDeputies(userId, INSTANCE_ID, permission)).thenReturn(true);
    }

    private static ProcessNodeExecutionInitContext<InstanceAssignmentActionNodeV1.Config> initContext(
            InstanceAssignmentActionNodeV1.Config config) {
        return new ProcessNodeExecutionInitContext<>(mock(ProcessNodeExecutionLogger.class),
                new ProcessNodeEntity().setId(12).setProcessId(42).setProcessVersion(3),
                new ProcessInstanceEntity().setId(INSTANCE_ID),
                new ProcessInstanceTaskEntity().setId(17L).setProcessData(Map.of()).setRuntimeData(Map.of()),
                null, new ProcessExecutionData(), config);
    }

    private static InstanceAssignmentActionNodeV1.Config config(String mode, String selectedUserId) {
        var config = new InstanceAssignmentActionNodeV1.Config();
        config.mode = mode;
        var circle = new AssignmentContextInputElementValue()
                .setDomainAndUserSelection(List.of(new DomainAndUserSelectInputElementValue("user", selectedUserId)));
        config.automaticAssignmentContext = circle;
        config.manualAssignmentContext = circle;
        return config;
    }

    private static UserEntity user(String id) {
        return new UserEntity().setId(id).setFullName(id).setEnabled(true).setDeletedInIdp(false);
    }
}
