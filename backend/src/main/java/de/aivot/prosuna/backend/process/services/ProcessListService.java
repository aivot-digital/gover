package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.utils.CrockfordCaseNumber;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.dtos.ProcessListDTO;
import de.aivot.prosuna.backend.process.dtos.ProcessListFilter;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;

@Service
@Transactional(readOnly = true)
public class ProcessListService {
    public static final List<ProcessTaskStatus> OPEN_TASK_STATUSES = List.of(ProcessTaskStatus.Running,
            ProcessTaskStatus.Paused, ProcessTaskStatus.AwaitingCustomer, ProcessTaskStatus.AwaitingPayment);
    private final EntityManager em;
    private final PermissionService permissions;
    private final UserRepository users;
    private final ProcessNodeDefinitionService definitions;

    public ProcessListService(@Nonnull EntityManager em, @Nonnull PermissionService permissions, @Nonnull UserRepository users,
                              @Nonnull ProcessNodeDefinitionService definitions) {
        this.em = em;
        this.permissions = permissions;
        this.users = users;
        this.definitions = definitions;
    }

    @Nonnull
    public Page<ProcessListDTO.Instance> instances(@Nonnull String userId, @Nonnull Pageable page,
                                                   @Nonnull ProcessListFilter filter) throws ResponseException {
        var rows = rows(userId, page, filter, false);
        var names = assigneeNames(rows.getContent());
        return rows.map(row -> new ProcessListDTO.Instance(row.get("id", Long.class), row.get("caseNumber", String.class),
                fileNumbers(row), row.get("processId", Integer.class), row.get("processVersion", Integer.class),
                row.get("processName", String.class), row.get("assignedUserId", String.class), row.get("assignedUserId") == null ? null : names.get(row.get("assignedUserId")),
                row.get("status", ProcessInstanceStatus.class), row.get("statusOverride", String.class),
                row.get("started", Instant.class), row.get("finished", Instant.class), row.get("testClaim") != null));
    }

    @Nonnull
    public Page<ProcessListDTO.Task> tasks(@Nonnull String userId, @Nonnull Pageable page,
                                           @Nonnull ProcessListFilter filter) throws ResponseException {
        var rows = rows(userId, page, filter, true);
        var names = assigneeNames(rows.getContent());
        return rows.map(row -> {
            var definition = definitions.getProcessNodeDefinition(row.get("definitionKey", String.class), row.get("definitionVersion", Integer.class));
            var type = definition.map(value -> value.getName()).orElse(null);
            var name = nonBlank(row.get("taskName", String.class), nonBlank(type, "Unbenannte Aufgabe"));
            var description = nonBlank(row.get("description", String.class), definition.map(value -> value.getAbstract()).orElse(null));
            return new ProcessListDTO.Task(row.get("id", Long.class), row.get("instanceId", Long.class),
                    row.get("caseNumber", String.class), fileNumbers(row), row.get("processId", Integer.class),
                    row.get("processVersion", Integer.class), row.get("processName", String.class), name, type, description,
                    row.get("assignedUserId", String.class), row.get("assignedUserId") == null ? null : names.get(row.get("assignedUserId")),
                    row.get("status", ProcessTaskStatus.class), row.get("statusOverride", String.class),
                    row.get("started", Instant.class), row.get("deadline", Instant.class), row.get("finished", Instant.class),
                    row.get("testClaim") != null);
        });
    }

    @Nonnull
    public ProcessListDTO.Options options(@Nonnull String userId, boolean tasks, @Nullable Long instanceId) throws ResponseException {
        var filter = new ProcessListFilter(null, "all", null, null, instanceId, "all");
        var access = access(userId, instanceId);
        var cb = em.getCriteriaBuilder();
        var query = cb.createTupleQuery();
        var roots = roots(query, tasks);
        var owner = roots.owner();
        // Filter choices describe the authorized scope, independently of the current page/search/status.
        query.multiselect(roots.process.get("id").alias("processId"), roots.process.get("internalTitle").alias("processName"),
                        owner.get("assignedUserId").alias("assignedUserId"))
                .where(predicates(cb, roots, filter, userId, access, Instant.now())).distinct(true);
        var result = em.createQuery(query).getResultList();
        var processes = new TreeMap<String, String>();
        for (var row : result) processes.put(row.get("processId").toString(), row.get("processName", String.class));
        var names = assigneeNames(result);
        return new ProcessListDTO.Options(processes.entrySet().stream()
                .map(entry -> new ProcessListDTO.Option(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ProcessListDTO.Option::label, String.CASE_INSENSITIVE_ORDER)).toList(),
                names.entrySet().stream().map(entry -> new ProcessListDTO.Option(entry.getKey(), entry.getValue()))
                        .sorted(Comparator.comparing(ProcessListDTO.Option::label, String.CASE_INSENSITIVE_ORDER)).toList());
    }

    public long countOpenAssignedTasks(@Nonnull String userId) throws ResponseException {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var roots = roots(query, true);
        query.select(cb.count(roots.task)).where(predicates(cb, roots,
                new ProcessListFilter(null, "open", null, null, null, "mine"), userId, access(userId, null), Instant.now()));
        return em.createQuery(query).getSingleResult();
    }

    @Nonnull
    private Page<Tuple> rows(@Nonnull String userId, @Nonnull Pageable page, @Nonnull ProcessListFilter filter, boolean tasks) throws ResponseException {
        var access = access(userId, filter.instanceId());
        var now = Instant.now();
        var cb = em.getCriteriaBuilder();
        var query = cb.createTupleQuery();
        var r = roots(query, tasks);
        var owner = r.owner();
        var columns = new ArrayList<Selection<?>>();
        columns.add(owner.get("id").alias("id"));
        if (tasks) columns.add(r.instance.get("id").alias("instanceId"));
        columns.add(r.instance.get("caseNumber").alias("caseNumber"));
        columns.add(r.instance.get("assignedFileNumbers").alias("fileNumbers"));
        columns.add(r.process.get("id").alias("processId"));
        columns.add((tasks ? owner.get("processVersion") : owner.get("initialProcessVersion")).alias("processVersion"));
        columns.add(r.process.get("internalTitle").alias("processName"));
        for (var field : List.of("assignedUserId", "status", "statusOverride", "started", "finished")) columns.add(owner.get(field).alias(field));
        columns.add(r.instance.get("createdForTestClaimId").alias("testClaim"));
        if (tasks) {
            columns.add(owner.get("deadline").alias("deadline"));
            columns.add(r.node.get("name").alias("taskName"));
            columns.add(r.node.get("description").alias("description"));
            columns.add(r.node.get("processNodeDefinitionKey").alias("definitionKey"));
            columns.add(r.node.get("processNodeDefinitionVersion").alias("definitionVersion"));
        }
        query.multiselect(columns).where(predicates(cb, r, filter, userId, access, now));
        var orders = new ArrayList<Order>();
        for (var order : page.getSort()) {
            Expression<?> field = switch (order.getProperty()) {
                case "id", "started", "finished", "status" -> owner.get(order.getProperty());
                case "caseNumber" -> r.instance.get("caseNumber");
                case "processName" -> cb.lower(r.process.get("internalTitle"));
                case "deadline" -> tasks ? owner.get("deadline") : null;
                default -> null;
            };
            if (field == null) throw ResponseException.badRequest("Nach dieser Spalte kann nicht sortiert werden.");
            // Missing dates belong at the end regardless of sort direction.
            orders.add(cb.asc(cb.selectCase().when(cb.isNull(field), 1).otherwise(0)));
            orders.add(order.isAscending() ? cb.asc(field) : cb.desc(field));
        }
        if (orders.isEmpty()) orders.add(cb.desc(owner.get("started")));
        if (page.getSort().getOrderFor("deadline") != null) orders.add(cb.asc(owner.get("started")));
        orders.add(cb.asc(owner.get("id")));
        query.orderBy(orders);
        var items = em.createQuery(query).setFirstResult(Math.toIntExact(page.getOffset())).setMaxResults(page.getPageSize()).getResultList();
        var countQuery = cb.createQuery(Long.class);
        var countRoots = roots(countQuery, tasks);
        countQuery.select(cb.count(countRoots.owner())).where(predicates(cb, countRoots, filter, userId, access, now));
        return new PageImpl<>(items, page, em.createQuery(countQuery).getSingleResult());
    }

    @Nullable
    private List<Long> access(@Nonnull String userId, @Nullable Long instanceId) throws ResponseException {
        if (instanceId != null) permissions.requireProcessInstancePermission(userId, instanceId, PROCESS_INSTANCE_READ);
        return permissions.hasSystemPermission(userId, PROCESS_INSTANCE_READ) ? null
                : permissions.getProcessInstancesWithPermission(userId, PROCESS_INSTANCE_READ);
    }

    private record Roots(@Nonnull Root<ProcessInstanceEntity> instance, @Nonnull Root<ProcessEntity> process,
                         @Nullable Root<ProcessInstanceTaskEntity> task, @Nullable Root<ProcessNodeEntity> node) {
        @Nonnull
        Root<?> owner() {
            return task == null ? instance : task;
        }
    }

    @Nonnull
    private Roots roots(@Nonnull CriteriaQuery<?> query, boolean tasks) {
        return new Roots(query.from(ProcessInstanceEntity.class), query.from(ProcessEntity.class),
                tasks ? query.from(ProcessInstanceTaskEntity.class) : null, tasks ? query.from(ProcessNodeEntity.class) : null);
    }

    @Nonnull
    private Predicate[] predicates(@Nonnull CriteriaBuilder cb, @Nonnull Roots r, @Nonnull ProcessListFilter filter, @Nonnull String userId,
                                   @Nullable List<Long> access, @Nonnull Instant now) throws ResponseException {
        var result = new ArrayList<Predicate>();
        result.add(cb.equal(r.instance.get("processId"), r.process.get("id")));
        if (r.task != null) {
            result.add(cb.equal(r.task.get("processInstanceId"), r.instance.get("id")));
            result.add(cb.equal(r.task.get("processNodeId"), r.node.get("id")));
        }
        if (access != null) result.add(access.isEmpty() ? cb.disjunction() : r.instance.get("id").in(access));
        if (filter.instanceId() != null) result.add(cb.equal(r.instance.get("id"), filter.instanceId()));
        if (filter.processId() != null) result.add(cb.equal(r.process.get("id"), filter.processId()));
        if (filter.processVersion() != null) result.add(cb.equal(r.owner().get(r.task == null ? "initialProcessVersion" : "processVersion"), filter.processVersion()));
        var assignee = nonBlank(filter.assignee(), "all");
        if (assignee.equals("unassigned")) result.add(cb.isNull(r.owner().get("assignedUserId")));
        else if (!assignee.equals("all")) result.add(cb.equal(r.owner().get("assignedUserId"), assignee.equals("mine") ? userId : assignee));
        if (filter.search() != null && !filter.search().isBlank()) {
            var pattern = "%" + escapeLike(filter.search().trim().toLowerCase(Locale.ROOT)) + "%";
            var numbers = cb.function("array_to_string", String.class, r.instance.get("assignedFileNumbers"), cb.literal("\n"));
            var normalized = CrockfordCaseNumber.normalizeSearch(filter.search());
            var compactMatch = normalized == null ? cb.disjunction() : cb.like(
                    cb.function("compact_case_number_search_key", String.class, r.instance.get("caseNumber")),
                    "%" + normalized + "%");
            result.add(cb.or(cb.like(cb.lower(r.instance.get("caseNumber")), pattern, '\\'),
                    cb.like(cb.lower(numbers), pattern, '\\'), compactMatch));
        }
        var view = nonBlank(filter.view(), "all");
        if (r.task == null) {
            switch (view) {
                case "all" -> {
                }
                case "active" -> result.add(r.instance.get("status").in(ProcessInstanceStatus.Created, ProcessInstanceStatus.Running, ProcessInstanceStatus.Paused));
                case "ended" -> result.add(r.instance.get("status").in(ProcessInstanceStatus.Completed, ProcessInstanceStatus.Aborted));
                case "failed" -> result.add(cb.equal(r.instance.get("status"), ProcessInstanceStatus.Failed));
                default -> throw ResponseException.badRequest("Diese Vorgangsansicht ist nicht verfügbar.");
            }
        } else {
            switch (view) {
                case "all" -> {
                }
                case "open", "overdue" -> {
                    result.add(r.task.get("status").in(OPEN_TASK_STATUSES));
                    if (view.equals("overdue")) result.add(cb.lessThan(r.task.get("deadline"), now));
                }
                case "failed" -> result.add(cb.equal(r.task.get("status"), ProcessTaskStatus.Failed));
                default -> throw ResponseException.badRequest("Diese Aufgabenansicht ist nicht verfügbar.");
            }
        }
        return result.toArray(Predicate[]::new);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private List<String> fileNumbers(@Nonnull Tuple row) {
        return (List<String>) row.get("fileNumbers");
    }

    @Nonnull
    private Map<String, String> assigneeNames(@Nonnull List<Tuple> rows) {
        var ids = rows.stream().map(row -> row.get("assignedUserId", String.class)).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        // Names of assigned people are list metadata; no user profiles or additional staff directory access are exposed.
        var names = users.findAllById(ids).stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getFullName));
        ids.forEach(id -> names.putIfAbsent(id, "Name nicht verfügbar"));
        return names;
    }

    @Nonnull
    private static String escapeLike(@Nonnull String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Nullable
    private static String nonBlank(@Nullable String value, @Nullable String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
