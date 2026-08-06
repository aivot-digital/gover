package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.process.models.ProcessInstanceAccessSelectableItem;
import de.aivot.gover.backend.process.repositories.VPotentialProcessInstanceAccessRepository;
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
    void listSelectableItems_ReturnsDomainsTeamsAndDirectUsersWithRequiredProcessAccess() {
        rows = List.of(
                departmentRow(10, "Ordnungsamt", 1, List.of(REQUIRED_PERMISSION)),
                teamRow(20, "Bürgerbüro", List.of(REQUIRED_PERMISSION)),
                teamRow(30, "Leseteam", List.of("process_instance.read")),
                userRow("direct-dept-user", "Müller, Anna", "anna.mueller@example.test", 10, null, true, List.of(REQUIRED_PERMISSION)),
                userRow("direct-team-user", "Schmidt, Ben", "ben.schmidt@example.test", null, 20, true, List.of(REQUIRED_PERMISSION)),
                userRow("team-without-process-permission-user", "Klein, Carla", "carla.klein@example.test", null, 30, true, List.of()),
                userRow("indirect-member-user", "Meyer, Dora", "dora.meyer@example.test", 10, null, false, List.of(REQUIRED_PERMISSION))
        );

        var result = service.listSelectableItems(
                PROCESS_ID,
                PROCESS_VERSION,
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(
                List.of(
                        new ProcessInstanceAccessSelectableItem("orgUnit", "10", "Ordnungsamt", null, 1, 1),
                        new ProcessInstanceAccessSelectableItem("team", "20", "Bürgerbüro", "Team", null, 1),
                        new ProcessInstanceAccessSelectableItem("user", "direct-dept-user", "Müller, Anna", "anna.mueller@example.test", null),
                        new ProcessInstanceAccessSelectableItem("user", "direct-team-user", "Schmidt, Ben", "ben.schmidt@example.test", null)
                ),
                result
        );
    }

    @Test
    void listSelectableItems_DeduplicatesUserFromMultipleAuthorizedTeams() {
        rows = List.of(
                teamRow(20, "Bürgerbüro Nord", List.of(REQUIRED_PERMISSION)),
                teamRow(21, "Bürgerbüro Süd", List.of(REQUIRED_PERMISSION)),
                userRow("same-user", "Müller, Anna", "anna.mueller@example.test", null, 20, true, List.of(REQUIRED_PERMISSION)),
                userRow("same-user", "Müller, Anna", "anna.mueller@example.test", null, 21, true, List.of(REQUIRED_PERMISSION))
        );

        var result = service.listSelectableItems(
                PROCESS_ID,
                PROCESS_VERSION,
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(
                List.of(
                        new ProcessInstanceAccessSelectableItem("team", "20", "Bürgerbüro Nord", "Team", null, 1),
                        new ProcessInstanceAccessSelectableItem("team", "21", "Bürgerbüro Süd", "Team", null, 1),
                        new ProcessInstanceAccessSelectableItem("user", "same-user", "Müller, Anna", "anna.mueller@example.test", null)
                ),
                result
        );
    }

    @Test
    void listSelectableItems_ReturnsAuthorizedDomainsWithoutMatchingDirectUser() {
        rows = List.of(
                departmentRow(10, "Ordnungsamt", 1, List.of(REQUIRED_PERMISSION)),
                departmentRow(11, "Personalamt", 1, List.of(REQUIRED_PERMISSION)),
                teamRow(20, "Bürgerbüro", List.of(REQUIRED_PERMISSION)),
                teamRow(21, "Archiv", List.of(REQUIRED_PERMISSION)),
                userRow("disabled-user", "Klein, Carla", "carla.klein@example.test", 10, null, true, List.of(REQUIRED_PERMISSION), false),
                userRow("indirect-user", "Meyer, Dora", "dora.meyer@example.test", null, 20, false, List.of(REQUIRED_PERMISSION))
        );

        var result = service.listSelectableItems(
                PROCESS_ID,
                PROCESS_VERSION,
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(
                List.of(
                        new ProcessInstanceAccessSelectableItem("orgUnit", "10", "Ordnungsamt", null, 1, 0),
                        new ProcessInstanceAccessSelectableItem("orgUnit", "11", "Personalamt", null, 1, 0),
                        new ProcessInstanceAccessSelectableItem("team", "20", "Bürgerbüro", "Team", null, 0),
                        new ProcessInstanceAccessSelectableItem("team", "21", "Archiv", "Team", null, 0)
                ),
                result
        );
    }

    @Test
    void listSelectableItems_IgnoresNullRows() {
        rows = Arrays.asList(
                null,
                departmentRow(10, "Ordnungsamt", 1, List.of(REQUIRED_PERMISSION)),
                userRow("dept-user", "Müller, Anna", "anna.mueller@example.test", 10, null, true, List.of(REQUIRED_PERMISSION))
        );

        var result = service.listSelectableItems(
                PROCESS_ID,
                PROCESS_VERSION,
                List.of(REQUIRED_PERMISSION)
        );

        assertEquals(
                List.of(
                        new ProcessInstanceAccessSelectableItem("orgUnit", "10", "Ordnungsamt", null, 1, 1),
                        new ProcessInstanceAccessSelectableItem("user", "dept-user", "Müller, Anna", "anna.mueller@example.test", null)
                ),
                result
        );
    }

    private static Object[] departmentRow(Integer departmentId,
                                          String departmentLabel,
                                          Integer departmentDepth,
                                          List<String> permissions) {
        return new Object[]{
                departmentId,
                departmentLabel,
                departmentDepth,
                null,
                null,
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

    private static Object[] teamRow(Integer teamId,
                                    String teamLabel,
                                    List<String> permissions) {
        return new Object[]{
                null,
                null,
                null,
                teamId,
                teamLabel,
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

    private static Object[] userRow(String userId,
                                    String userLabel,
                                    String userSubLabel,
                                    Integer viaDepartmentId,
                                    Integer viaTeamId,
                                    boolean isDirectMember,
                                    List<String> permissions) {
        return userRow(userId, userLabel, userSubLabel, viaDepartmentId, viaTeamId, isDirectMember, permissions, true);
    }

    private static Object[] userRow(String userId,
                                    String userLabel,
                                    String userSubLabel,
                                    Integer viaDepartmentId,
                                    Integer viaTeamId,
                                    boolean isDirectMember,
                                    List<String> permissions,
                                    boolean isEnabled) {
        return new Object[]{
                null,
                null,
                null,
                null,
                null,
                userId,
                isEnabled,
                userLabel,
                userSubLabel,
                viaDepartmentId,
                viaTeamId,
                isDirectMember,
                permissions.toArray(String[]::new)
        };
    }

    private final VPotentialProcessInstanceAccessRepository repository = proxy(
            VPotentialProcessInstanceAccessRepository.class,
            (methodName, args) -> switch (methodName) {
                case "findSelectableRowsByProcessIdAndProcessVersion" -> rows;
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
