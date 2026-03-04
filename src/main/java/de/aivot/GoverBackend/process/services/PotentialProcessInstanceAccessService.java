package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.process.models.ProcessInstanceAccessSelectableItem;
import de.aivot.GoverBackend.process.repositories.VPotentialProcessInstanceAccessRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
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

        var rows = repository.findRowsByProcessIdAndProcessVersion(processId, processVersion)
                .stream()
                .map(PotentialProcessInstanceAccessService::toRow)
                .filter(Objects::nonNull)
                .toList();

        var matchingUsers = rows
                .stream()
                .filter(PotentialProcessInstanceAccessService::isUserRow)
                .filter(row -> Boolean.TRUE.equals(row.userIsEnabled()))
                .filter(row -> hasRequiredPermissions(row.permissions(), normalizedRequiredPermissions))
                .toList();

        var departmentsWithMatchingUsers = matchingUsers
                .stream()
                .map(PotentialAccessRow::userViaDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        var teamsWithMatchingUsers = matchingUsers
                .stream()
                .map(PotentialAccessRow::userViaTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        var selectableItems = new LinkedHashMap<String, ProcessInstanceAccessSelectableItem>();

        for (var row : rows) {
            if (isUserRow(row)) {
                if (Boolean.TRUE.equals(row.userIsEnabled()) &&
                    hasRequiredPermissions(row.permissions(), normalizedRequiredPermissions)) {
                    putItem(selectableItems, "user", row.userId());
                }
                continue;
            }

            if (row.departmentId() != null &&
                (hasRequiredPermissions(row.permissions(), normalizedRequiredPermissions) ||
                 departmentsWithMatchingUsers.contains(row.departmentId()))) {
                putItem(selectableItems, "orgUnit", row.departmentId().toString());
            }

            if (row.teamId() != null &&
                (hasRequiredPermissions(row.permissions(), normalizedRequiredPermissions) ||
                 teamsWithMatchingUsers.contains(row.teamId()))) {
                putItem(selectableItems, "team", row.teamId().toString());
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
            @Nonnull String id
    ) {
        var normalizedId = id.trim();
        if (normalizedId.isBlank()) {
            return;
        }

        itemMap.put(type + ":" + normalizedId, new ProcessInstanceAccessSelectableItem(type, normalizedId));
    }

    @Nullable
    private static PotentialAccessRow toRow(@Nullable Object[] row) {
        if (row == null || row.length < 7) {
            return null;
        }

        return new PotentialAccessRow(
                toInteger(row[0]),
                toInteger(row[1]),
                toStringValue(row[2]),
                toBoolean(row[3]),
                toInteger(row[4]),
                toInteger(row[5]),
                toStringList(row[6])
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
            @Nullable Integer teamId,
            @Nullable String userId,
            @Nullable Boolean userIsEnabled,
            @Nullable Integer userViaDepartmentId,
            @Nullable Integer userViaTeamId,
            @Nonnull List<String> permissions
    ) {
    }
}
