package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.GoverBackend.elements.models.elements.form.input.DomainAndUserSelectInputElementValue;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.process.repositories.VPotentialProcessInstanceAccessRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AssignmentContextAssigneeResolverService {
    private static final List<ProcessTaskStatus> ACTIVE_TASK_STATUSES = List.of(
            ProcessTaskStatus.Running,
            ProcessTaskStatus.Paused
    );

    private final VPotentialProcessInstanceAccessRepository potentialProcessInstanceAccessRepository;
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;

    public AssignmentContextAssigneeResolverService(
            VPotentialProcessInstanceAccessRepository potentialProcessInstanceAccessRepository,
            ProcessInstanceTaskRepository processInstanceTaskRepository
    ) {
        this.potentialProcessInstanceAccessRepository = potentialProcessInstanceAccessRepository;
        this.processInstanceTaskRepository = processInstanceTaskRepository;
    }

    @Nonnull
    public java.util.Optional<String> resolveAssignee(
            @Nonnull Integer processId,
            @Nonnull Integer processVersion,
            @Nonnull Long processInstanceId,
            @Nullable Integer previousProcessNodeId,
            @Nullable String processInstanceAssignedUserId,
            @Nullable AssignmentContextInputElementValue assignmentContext,
            @Nullable List<String> requiredPermissions
    ) {
        var selection = assignmentContext != null ? assignmentContext.getDomainAndUserSelection() : null;
        if (selection == null || selection.isEmpty()) {
            return java.util.Optional.empty();
        }

        var normalizedRequiredPermissions = normalizePermissions(requiredPermissions);
        var accessRows = potentialProcessInstanceAccessRepository
                .findRowsByProcessIdAndProcessVersion(processId, processVersion)
                .stream()
                .map(AssignmentContextAssigneeResolverService::toAccessRow)
                .filter(Objects::nonNull)
                .toList();

        var eligibleUserRows = accessRows
                .stream()
                .filter(PotentialAccessRow::isUserRow)
                .filter(row -> Boolean.TRUE.equals(row.userIsEnabled()))
                .filter(row -> Boolean.TRUE.equals(row.userIsDirectMember()))
                .filter(row -> hasRequiredPermissions(row.userDirectPermissions(), normalizedRequiredPermissions))
                .toList();

        if (eligibleUserRows.isEmpty()) {
            return java.util.Optional.empty();
        }

        var eligibleUserIds = eligibleUserRows
                .stream()
                .map(PotentialAccessRow::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        var candidateUserIds = collectCandidateUserIds(selection, eligibleUserRows, eligibleUserIds);
        if (candidateUserIds.isEmpty()) {
            return java.util.Optional.empty();
        }

        var preferredCandidateIds = applyPreference(
                candidateUserIds,
                assignmentContext,
                processInstanceId,
                previousProcessNodeId,
                processInstanceAssignedUserId
        );

        var activeTaskCounts = determineActiveTaskCounts(preferredCandidateIds);

        return preferredCandidateIds
                .stream()
                .sorted(Comparator
                        .comparingLong((String userId) -> activeTaskCounts.getOrDefault(userId, 0L))
                        .thenComparing(Function.identity()))
                .findFirst();
    }

    @Nonnull
    private LinkedHashSet<String> collectCandidateUserIds(
            @Nonnull List<DomainAndUserSelectInputElementValue> selection,
            @Nonnull List<PotentialAccessRow> eligibleUserRows,
            @Nonnull Set<String> eligibleUserIds
    ) {
        var candidateUserIds = new LinkedHashSet<String>();

        for (var item : selection) {
            if (item == null || item.isEmpty()) {
                continue;
            }

            switch (item.getType()) {
                case "user" -> {
                    if (item.getId() != null && eligibleUserIds.contains(item.getId())) {
                        candidateUserIds.add(item.getId());
                    }
                }
                case "orgUnit" -> {
                    var departmentId = parseInteger(item.getId());
                    if (departmentId != null) {
                        eligibleUserRows
                                .stream()
                                .filter(row -> Objects.equals(row.userViaDepartmentId(), departmentId))
                                .map(PotentialAccessRow::userId)
                                .filter(Objects::nonNull)
                                .forEach(candidateUserIds::add);
                    }
                }
                case "team" -> {
                    var teamId = parseInteger(item.getId());
                    if (teamId != null) {
                        eligibleUserRows
                                .stream()
                                .filter(row -> Objects.equals(row.userViaTeamId(), teamId))
                                .map(PotentialAccessRow::userId)
                                .filter(Objects::nonNull)
                                .forEach(candidateUserIds::add);
                    }
                }
                default -> {
                    // Ignore unsupported values defensively.
                }
            }
        }

        return candidateUserIds;
    }

    @Nonnull
    private LinkedHashSet<String> applyPreference(
            @Nonnull LinkedHashSet<String> candidateUserIds,
            @Nullable AssignmentContextInputElementValue assignmentContext,
            @Nonnull Long processInstanceId,
            @Nullable Integer previousProcessNodeId,
            @Nullable String processInstanceAssignedUserId
    ) {
        if (assignmentContext == null || candidateUserIds.isEmpty()) {
            return candidateUserIds;
        }

        if (Boolean.TRUE.equals(assignmentContext.getPreferPreviousTaskAssignee())) {
            var preferredUserId = resolvePreviousTaskAssignee(processInstanceId, previousProcessNodeId);
            if (preferredUserId != null && candidateUserIds.contains(preferredUserId)) {
                return new LinkedHashSet<>(List.of(preferredUserId));
            }
        }

        if (Boolean.TRUE.equals(assignmentContext.getPreferProcessInstanceAssignee())) {
            if (processInstanceAssignedUserId != null && candidateUserIds.contains(processInstanceAssignedUserId)) {
                return new LinkedHashSet<>(List.of(processInstanceAssignedUserId));
            }
        }

        if (Boolean.TRUE.equals(assignmentContext.getPreferUninvolvedUser())) {
            var involvedUserIds = processInstanceTaskRepository
                    .findAllByProcessInstanceId(processInstanceId)
                    .stream()
                    .map(ProcessInstanceTaskEntity::getAssignedUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            var uninvolvedUserIds = candidateUserIds
                    .stream()
                    .filter(userId -> !involvedUserIds.contains(userId))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (!uninvolvedUserIds.isEmpty()) {
                return uninvolvedUserIds;
            }
        }

        return candidateUserIds;
    }

    @Nullable
    private String resolvePreviousTaskAssignee(@Nonnull Long processInstanceId,
                                               @Nullable Integer previousProcessNodeId) {
        if (previousProcessNodeId == null) {
            return null;
        }

        var previousTask = processInstanceTaskRepository
                .findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc(processInstanceId, previousProcessNodeId)
                .orElse(null);

        return previousTask != null ? previousTask.getAssignedUserId() : null;
    }

    @Nonnull
    private Map<String, Long> determineActiveTaskCounts(@Nonnull Set<String> candidateUserIds) {
        if (candidateUserIds.isEmpty()) {
            return Map.of();
        }

        return processInstanceTaskRepository
                .findAllByAssignedUserIdInAndStatusIn(candidateUserIds, ACTIVE_TASK_STATUSES)
                .stream()
                .map(ProcessInstanceTaskEntity::getAssignedUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    @Nullable
    private static PotentialAccessRow toAccessRow(@Nullable Object[] row) {
        if (row == null || row.length < 9) {
            return null;
        }

        return new PotentialAccessRow(
                toInteger(row[0]),
                toInteger(row[1]),
                toStringValue(row[2]),
                toBoolean(row[3]),
                toInteger(row[4]),
                toInteger(row[5]),
                toBoolean(row[6]),
                toStringList(row[7]),
                toStringList(row[8])
        );
    }

    @Nonnull
    private static List<String> normalizePermissions(@Nullable List<String> permissions) {
        if (permissions == null) {
            return List.of();
        }

        return permissions
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(permission -> !permission.isBlank())
                .distinct()
                .toList();
    }

    private static boolean hasRequiredPermissions(
            @Nullable List<String> availablePermissions,
            @Nonnull List<String> requiredPermissions
    ) {
        if (requiredPermissions.isEmpty()) {
            return true;
        }

        if (availablePermissions == null || availablePermissions.isEmpty()) {
            return false;
        }

        var normalizedPermissions = availablePermissions
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(permission -> !permission.isBlank())
                .collect(Collectors.toSet());

        if (normalizedPermissions.contains("*")) {
            return true;
        }

        return normalizedPermissions.containsAll(requiredPermissions);
    }

    @Nullable
    private static Integer parseInteger(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private static Integer toInteger(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case Integer i -> i;
            case Number n -> n.intValue();
            default -> {
                try {
                    yield Integer.parseInt(value.toString());
                } catch (NumberFormatException ignored) {
                    yield null;
                }
            }
        };
    }

    @Nullable
    private static Boolean toBoolean(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case Boolean b -> b;
            case Number n -> n.intValue() != 0;
            default -> {
                var normalized = value.toString().trim().toLowerCase();
                if (normalized.isEmpty()) {
                    yield null;
                }

                if (normalized.equals("true") || normalized.equals("t") || normalized.equals("1") ||
                    normalized.equals("yes") || normalized.equals("y")) {
                    yield true;
                }

                if (normalized.equals("false") || normalized.equals("f") || normalized.equals("0") ||
                    normalized.equals("no") || normalized.equals("n")) {
                    yield false;
                }

                yield null;
            }
        };
    }

    @Nullable
    private static String toStringValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        var normalized = value.toString().trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Nonnull
    private static List<String> toStringList(@Nullable Object value) {
        if (value == null) {
            return List.of();
        }

        if (value instanceof Array sqlArray) {
            try {
                return toStringList(sqlArray.getArray());
            } catch (SQLException ignored) {
                return List.of();
            }
        }

        if (value instanceof String[] stringArray) {
            return normalizePermissions(java.util.Arrays.asList(stringArray));
        }

        if (value instanceof Object[] objectArray) {
            return normalizePermissions(
                    java.util.Arrays.stream(objectArray)
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .toList()
            );
        }

        if (value instanceof Collection<?> collection) {
            return normalizePermissions(
                    collection
                            .stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .toList()
            );
        }

        if (value instanceof String rawString) {
            return parsePostgresTextArray(rawString);
        }

        return normalizePermissions(List.of(value.toString().trim()));
    }

    @Nonnull
    private static List<String> parsePostgresTextArray(@Nullable String rawValue) {
        if (rawValue == null) {
            return List.of();
        }

        var trimmed = rawValue.trim();
        if (trimmed.isBlank()) {
            return List.of();
        }

        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return normalizePermissions(List.of(trimmed));
        }

        var inner = trimmed.substring(1, trimmed.length() - 1);
        if (inner.isBlank()) {
            return List.of();
        }

        return normalizePermissions(
                java.util.Arrays.stream(inner.split(","))
                        .map(String::trim)
                        .map(token -> token.replaceAll("^\"|\"$", ""))
                        .toList()
        );
    }

    private record PotentialAccessRow(
            @Nullable Integer departmentId,
            @Nullable Integer teamId,
            @Nullable String userId,
            @Nullable Boolean userIsEnabled,
            @Nullable Integer userViaDepartmentId,
            @Nullable Integer userViaTeamId,
            @Nullable Boolean userIsDirectMember,
            @Nonnull List<String> userDirectPermissions,
            @Nonnull List<String> permissions
    ) {
        private boolean isUserRow() {
            return userId != null && !userId.isBlank();
        }
    }
}
