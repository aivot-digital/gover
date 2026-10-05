package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.dtos.ProcessInstanceEventLogDTO;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProcessInstanceEventLogService {
    private final ProcessInstanceHistoryEventRepository eventRepository;
    private final ProcessInstanceRepository instanceRepository;
    private final ProcessInstanceTaskRepository taskRepository;
    private final ProcessNodeRepository nodeRepository;
    private final ProcessNodeDefinitionService nodeDefinitionService;
    private final UserRepository userRepository;

    public ProcessInstanceEventLogService(ProcessInstanceHistoryEventRepository eventRepository,
                                          ProcessInstanceRepository instanceRepository,
                                          ProcessInstanceTaskRepository taskRepository,
                                          ProcessNodeRepository nodeRepository,
                                          ProcessNodeDefinitionService nodeDefinitionService,
                                          UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.nodeRepository = nodeRepository;
        this.nodeDefinitionService = nodeDefinitionService;
        this.userRepository = userRepository;
    }

    @Nonnull
    public ProcessInstanceEventLogDTO getEventLog(long processInstanceId,
                                                   @Nullable Long processInstanceTaskId,
                                                   @Nullable String search,
                                                   boolean notableOnly,
                                                   @Nonnull Pageable pageable) throws ResponseException {
        if (search != null && search.length() > 200) {
            throw ResponseException.badRequest("Der Suchbegriff darf höchstens 200 Zeichen lang sein.");
        }

        var instance = instanceRepository
                .findById(processInstanceId)
                .orElseThrow(ResponseException::notFound);
        var task = processInstanceTaskId == null ? null : taskRepository
                .findById(processInstanceTaskId)
                .orElseThrow(ResponseException::notFound);

        if (task != null && !task.getProcessInstanceId().equals(processInstanceId)) {
            throw ResponseException.badRequest("Die Aufgabe gehört nicht zum angegebenen Vorgang.");
        }

        var eventPage = eventRepository.findAll(
                createSpecification(processInstanceId, processInstanceTaskId, search, notableOnly),
                normalizePageable(pageable)
        );

        // Resolve only references used by the current page. This avoids exposing or loading complete user and process lists.
        var tasksById = findTasksById(eventPage.getContent().stream()
                .map(ProcessInstanceEventEntity::getProcessInstanceTaskId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet()));
        var nodesById = findNodesById(tasksById.values().stream()
                .map(ProcessInstanceTaskEntity::getProcessNodeId)
                .collect(Collectors.toSet()));
        var usersById = findUsersById(eventPage.getContent().stream()
                .map(ProcessInstanceEventEntity::getTriggeringUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet()));
        var entries = eventPage.map(event -> toLogEntry(event, tasksById, nodesById, usersById));

        return new ProcessInstanceEventLogDTO(
                new ProcessInstanceEventLogDTO.InstanceContext(
                        instance.getId(),
                        instance.getCaseNumber(),
                        instance.getStarted(),
                        instance.getFinished(),
                        durationToMilliseconds(instance.getRuntime())
                ),
                task == null ? null : new ProcessInstanceEventLogDTO.TaskContext(
                        task.getId(),
                        resolveNodeName(nodeRepository.findById(task.getProcessNodeId()).orElse(null), "Aufgabe"),
                        task.getStarted(),
                        task.getFinished(),
                        durationToMilliseconds(task.getRuntime())
                ),
                entries
        );
    }

    @Nonnull
    private Specification<ProcessInstanceEventEntity> createSpecification(long processInstanceId,
                                                                            @Nullable Long processInstanceTaskId,
                                                                            @Nullable String search,
                                                                            boolean notableOnly) {
        Specification<ProcessInstanceEventEntity> specification = (root, query, builder) ->
                builder.equal(root.get("processInstanceId"), processInstanceId);

        if (processInstanceTaskId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("processInstanceTaskId"), processInstanceTaskId));
        }
        if (notableOnly) {
            specification = specification.and((root, query, builder) ->
                    root.get("level").in(ProcessNodeExecutionLogLevel.Warn, ProcessNodeExecutionLogLevel.Error));
        }

        var normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.GERMAN);
        if (normalizedSearch.isEmpty()) {
            return specification;
        }

        var matchingTaskIds = findMatchingTaskIds(processInstanceId, normalizedSearch);
        var matchingUserIds = userRepository.findIdsByFullNameContaining(normalizedSearch);
        var likeSearch = "%" + normalizedSearch + "%";

        return specification.and((root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.like(builder.lower(root.get("title")), likeSearch));
            predicates.add(builder.like(builder.lower(root.get("message")), likeSearch));

            if (!matchingTaskIds.isEmpty()) {
                predicates.add(root.get("processInstanceTaskId").in(matchingTaskIds));
            }
            if (!matchingUserIds.isEmpty()) {
                predicates.add(root.get("triggeringUserId").in(matchingUserIds));
            }
            if ("system".contains(normalizedSearch)) {
                predicates.add(builder.isNull(root.get("triggeringUserId")));
            }

            return builder.or(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        });
    }

    @Nonnull
    private Set<Long> findMatchingTaskIds(long processInstanceId, @Nonnull String normalizedSearch) {
        var tasks = taskRepository.findAllByProcessInstanceId(processInstanceId);
        var nodesById = findNodesById(tasks.stream()
                .map(ProcessInstanceTaskEntity::getProcessNodeId)
                .collect(Collectors.toSet()));

        return tasks.stream()
                .filter(task -> resolveNodeName(nodesById.get(task.getProcessNodeId()), "Aufgabe")
                        .toLowerCase(Locale.GERMAN)
                        .contains(normalizedSearch))
                .map(ProcessInstanceTaskEntity::getId)
                .collect(Collectors.toSet());
    }

    @Nonnull
    private ProcessInstanceEventLogDTO.Entry toLogEntry(
            @Nonnull ProcessInstanceEventEntity event,
            @Nonnull Map<Long, ProcessInstanceTaskEntity> tasksById,
            @Nonnull Map<Integer, ProcessNodeEntity> nodesById,
            @Nonnull Map<String, UserEntity> usersById
    ) {
        var task = event.getProcessInstanceTaskId() == null ? null : tasksById.get(event.getProcessInstanceTaskId());
        var node = task == null ? null : nodesById.get(task.getProcessNodeId());
        var triggeringUser = event.getTriggeringUserId() == null ? null : usersById.get(event.getTriggeringUserId());

        return new ProcessInstanceEventLogDTO.Entry(
                event.getId(),
                event.getProcessInstanceId(),
                event.getProcessInstanceTaskId(),
                event.getLevel(),
                event.getTechnical(),
                event.getAudit(),
                event.getTitle(),
                event.getMessage(),
                event.getDetails(),
                event.getTimestamp(),
                event.getTriggeringUserId(),
                triggeringUser == null ? null : resolveUserName(triggeringUser),
                node == null ? null : resolveNodeName(node, "Prozesselement")
        );
    }

    @Nonnull
    private String resolveUserName(@Nonnull UserEntity user) {
        var name = user.getFullName().isBlank() ? "Unbenannte Mitarbeiter:in" : user.getFullName();
        if (user.getDeletedInIdp()) {
            return name + " (gelöscht)";
        }
        if (!user.getEnabled()) {
            return name + " (inaktiv)";
        }
        return name;
    }

    @Nonnull
    private String resolveNodeName(@Nullable ProcessNodeEntity node, @Nonnull String fallback) {
        if (node == null) {
            return fallback;
        }

        return nodeDefinitionService
                .getProcessNodeDefinition(node)
                .map(node::resolveName)
                .filter(name -> !name.isBlank())
                .orElseGet(() -> node.getName() != null && !node.getName().isBlank() ? node.getName() : fallback);
    }

    @Nonnull
    private Map<Long, ProcessInstanceTaskEntity> findTasksById(@Nonnull Collection<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return taskRepository.findAllById(taskIds)
                .stream()
                .collect(Collectors.toMap(ProcessInstanceTaskEntity::getId, Function.identity()));
    }

    @Nonnull
    private Map<Integer, ProcessNodeEntity> findNodesById(@Nonnull Collection<Integer> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return nodeRepository.findAllById(nodeIds)
                .stream()
                .collect(Collectors.toMap(ProcessNodeEntity::getId, Function.identity()));
    }

    @Nonnull
    private Map<String, UserEntity> findUsersById(@Nonnull Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    @Nonnull
    private Pageable normalizePageable(@Nonnull Pageable pageable) {
        var timestampOrder = pageable.getSort().getOrderFor("timestamp");
        var direction = timestampOrder == null ? Sort.Direction.DESC : timestampOrder.getDirection();
        return PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), 100),
                Sort.by(direction, "timestamp")
        );
    }

    @Nullable
    private Long durationToMilliseconds(@Nullable Duration duration) {
        return duration == null ? null : duration.toMillis();
    }
}
