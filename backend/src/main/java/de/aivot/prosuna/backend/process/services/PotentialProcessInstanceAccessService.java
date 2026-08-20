package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.models.ProcessInstanceAccessSelectableItem;
import de.aivot.prosuna.backend.process.repositories.VPotentialProcessInstanceAccessRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PotentialProcessInstanceAccessService {
    private static final String PERMISSION_WILDCARD = "*";

    private final VPotentialProcessInstanceAccessRepository repository;

    @Autowired
    public PotentialProcessInstanceAccessService(VPotentialProcessInstanceAccessRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    public List<ProcessInstanceAccessSelectableItem> listSelectableItems(
            @Nonnull Integer processId,
            @Nonnull Integer processVersion,
            @Nullable List<String> requiredPermissions
    ) {
        var normalizedRequiredPermissions = normalizePermissions(requiredPermissions);

        var rows = repository.findSelectableRowsByProcessIdAndProcessVersion(processId, processVersion)
                .stream()
                .map(PotentialProcessInstanceAccessService::toRow)
                .filter(Objects::nonNull)
                .toList();

        var matchingUserRows = rows
                .stream()
                .filter(PotentialProcessInstanceAccessService::isUserRow)
                .filter(row -> Boolean.TRUE.equals(row.userIsEnabled()))
                .filter(row -> hasDirectUserProcessAccess(row, normalizedRequiredPermissions))
                .toList();

        var eligibleUserIdsByDepartment = matchingUserRows
                .stream()
                .filter(row -> row.userViaDepartmentId() != null)
                .filter(row -> row.userId() != null && !row.userId().isBlank())
                .collect(Collectors.groupingBy(
                        PotentialAccessRow::userViaDepartmentId,
                        Collectors.mapping(PotentialAccessRow::userId, Collectors.toSet())
                ));

        var eligibleUserIdsByTeam = matchingUserRows
                .stream()
                .filter(row -> row.userViaTeamId() != null)
                .filter(row -> row.userId() != null && !row.userId().isBlank())
                .collect(Collectors.groupingBy(
                        PotentialAccessRow::userViaTeamId,
                        Collectors.mapping(PotentialAccessRow::userId, Collectors.toSet())
                ));

        var selectableItems = new LinkedHashMap<String, ProcessInstanceAccessSelectableItem>();

        for (var row : rows) {
            if (isUserRow(row)) {
                if (Boolean.TRUE.equals(row.userIsEnabled()) &&
                    hasDirectUserProcessAccess(row, normalizedRequiredPermissions)) {
                    putItem(
                            selectableItems,
                            "user",
                            row.userId(),
                            row.userLabel(),
                            row.userSubLabel(),
                            null,
                            null
                    );
                }
                continue;
            }

            if (row.departmentId() != null &&
                hasRequiredPermissions(row.permissions(), normalizedRequiredPermissions)) {
                putItem(
                        selectableItems,
                        "orgUnit",
                        row.departmentId().toString(),
                        row.departmentLabel(),
                        null,
                        row.departmentDepth(),
                        countEligibleUsers(eligibleUserIdsByDepartment.get(row.departmentId()))
                );
            }

            if (row.teamId() != null &&
                hasRequiredPermissions(row.permissions(), normalizedRequiredPermissions)) {
                putItem(
                        selectableItems,
                        "team",
                        row.teamId().toString(),
                        row.teamLabel(),
                        "Team",
                        null,
                        countEligibleUsers(eligibleUserIdsByTeam.get(row.teamId()))
                );
            }
        }

        return selectableItems
                .values()
                .stream()
                .sorted(Comparator
                        .comparingInt((ProcessInstanceAccessSelectableItem item) -> switch (item.type()) {
                            case "orgUnit" -> 0;
                            case "team" -> 1;
                            case "user" -> 2;
                            default -> 99;
                        })
                        .thenComparing(ProcessInstanceAccessSelectableItem::id))
                .toList();
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

    private static boolean hasDirectUserProcessAccess(
            @Nonnull PotentialAccessRow row,
            @Nonnull List<String> requiredPermissions
    ) {
        return Boolean.TRUE.equals(row.userIsDirectMember()) &&
               hasRequiredPermissions(row.permissions(), requiredPermissions);
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

        if (normalizedPermissions.contains(PERMISSION_WILDCARD)) {
            return true;
        }

        return normalizedPermissions.containsAll(requiredPermissions);
    }

    private static boolean isUserRow(@Nullable PotentialAccessRow row) {
        if (row == null) {
            return false;
        }

        return row.userId() != null && !row.userId().isBlank();
    }

    private static void putItem(
            @Nonnull LinkedHashMap<String, ProcessInstanceAccessSelectableItem> itemMap,
            @Nonnull String type,
            @Nonnull String id,
            @Nullable String label,
            @Nullable String subLabel,
            @Nullable Integer departmentDepth,
            @Nullable Integer eligibleUserCount
    ) {
        var normalizedId = id.trim();
        if (normalizedId.isBlank()) {
            return;
        }

        var normalizedLabel = label == null || label.isBlank()
                ? fallbackLabel(type, normalizedId)
                : label.trim();

        itemMap.put(
                type + ":" + normalizedId,
                new ProcessInstanceAccessSelectableItem(
                        type,
                        normalizedId,
                        normalizedLabel,
                        subLabel == null || subLabel.isBlank() ? null : subLabel.trim(),
                        departmentDepth,
                        eligibleUserCount
                )
        );
    }

    private static int countEligibleUsers(@Nullable Set<String> userIds) {
        return userIds == null ? 0 : userIds.size();
    }

    @Nonnull
    private static String fallbackLabel(@Nonnull String type, @Nonnull String id) {
        return switch (type) {
            case "orgUnit" -> "Organisationseinheit #" + id;
            case "team" -> "Team #" + id;
            case "user" -> "Mitarbeiter:in #" + id;
            default -> type + ":" + id;
        };
    }

    @Nullable
    private static PotentialAccessRow toRow(@Nullable Object[] row) {
        if (row == null || row.length < 13) {
            return null;
        }

        return new PotentialAccessRow(
                toInteger(row[0]),
                toStringValue(row[1]),
                toInteger(row[2]),
                toInteger(row[3]),
                toStringValue(row[4]),
                toStringValue(row[5]),
                toBoolean(row[6]),
                toStringValue(row[7]),
                toStringValue(row[8]),
                toInteger(row[9]),
                toInteger(row[10]),
                toBoolean(row[11]),
                toStringList(row[12])
        );
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
            @Nullable String departmentLabel,
            @Nullable Integer departmentDepth,
            @Nullable Integer teamId,
            @Nullable String teamLabel,
            @Nullable String userId,
            @Nullable Boolean userIsEnabled,
            @Nullable String userLabel,
            @Nullable String userSubLabel,
            @Nullable Integer userViaDepartmentId,
            @Nullable Integer userViaTeamId,
            @Nullable Boolean userIsDirectMember,
            @Nonnull List<String> permissions
    ) {
    }
}
