package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.process.models.ProcessInstanceAccessSelectableItem;
import de.aivot.GoverBackend.process.repositories.VPotentialProcessInstanceAccessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PotentialProcessInstanceAccessServiceTest {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final String REQUIRED_PERMISSION = "process_instance.edit_task";

    private List<Object[]> rows;
    private PotentialProcessInstanceAccessService service;

    @BeforeEach
    void setUp() {
        rows = List.of();
        service = new PotentialProcessInstanceAccessService(repository);
    }

    @Test
    void listSelectableItems_UsesOnlyDirectMembershipAndDirectPermissionsForUsers() {
        rows = List.of(
                departmentRow(10, List.of()),
                teamRow(20, List.of()),
                userRow("direct-user", 10, null, true, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("indirect-member-user", 10, null, false, List.of(REQUIRED_PERMISSION), List.of(REQUIRED_PERMISSION)),
                userRow("indirect-permission-user", null, 20, true, List.of(), List.of(REQUIRED_PERMISSION))
        );

        var result = service.listSelectableItems(
                PROCESS_ID,
                PROCESS_VERSION,
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(
                List.of(
                        new ProcessInstanceAccessSelectableItem("orgUnit", "10"),
                        new ProcessInstanceAccessSelectableItem("user", "direct-user")
                ),
                result
        );
    }

    @Test
    void listSelectableItems_KeepsDirectDepartmentAndTeamAccess() {
        rows = List.of(
                departmentRow(10, List.of(REQUIRED_PERMISSION)),
                teamRow(20, List.of(REQUIRED_PERMISSION)),
                userRow("deputy-only-user", 10, null, false, List.of(), List.of(REQUIRED_PERMISSION))
        );

        var result = service.listSelectableItems(
                PROCESS_ID,
                PROCESS_VERSION,
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(
                List.of(
                        new ProcessInstanceAccessSelectableItem("orgUnit", "10"),
                        new ProcessInstanceAccessSelectableItem("team", "20")
                ),
                result
        );
    }

    @Test
    void listSelectableItems_IgnoresNullRows() {
        rows = Arrays.asList(
                null,
                departmentRow(10, List.of(REQUIRED_PERMISSION))
        );

        var result = service.listSelectableItems(
                PROCESS_ID,
                PROCESS_VERSION,
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(
                List.of(new ProcessInstanceAccessSelectableItem("orgUnit", "10")),
                result
        );
    }

    private static Object[] departmentRow(Integer departmentId, List<String> permissions) {
        return new Object[]{
                departmentId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                permissions.toArray(String[]::new)
        };
    }

    private static Object[] teamRow(Integer teamId, List<String> permissions) {
        return new Object[]{
                null,
                teamId,
                null,
                null,
                null,
                null,
                null,
                null,
                permissions.toArray(String[]::new)
        };
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

    private VPotentialProcessInstanceAccessRepository repository = proxy(
            VPotentialProcessInstanceAccessRepository.class,
            (methodName, args) -> switch (methodName) {
                case "findRowsByProcessIdAndProcessVersion" -> rows;
                default -> unsupported(methodName);
            }
    );

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
