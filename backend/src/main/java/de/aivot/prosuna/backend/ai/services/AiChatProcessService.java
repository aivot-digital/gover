package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.models.AiProcessChatContext;
import de.aivot.prosuna.backend.ai.models.AiProcessConfigurationChange;
import de.aivot.prosuna.backend.ai.permissions.AiChatPermissionProvider;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import de.aivot.prosuna.backend.audit.enums.AuditAction;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.*;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;

@Service
@Transactional(readOnly = true, rollbackFor = ResponseException.class)
public class AiChatProcessService {
    private static final Set<String> EDITABLE = Set.of("name", "description", "dataKey", "outputMappings", "timeLimitDays", "notes", "requirements");
    private final PermissionService permissions;
    private final AiChatSessionRepository sessions;
    private final UserService users;
    private final ProcessVersionRepository versions;
    private final ProcessNodeRepository nodes;
    private final ProcessEdgeRepository edges;
    private final ProcessNodeService nodeService;
    private final ProcessEdgeService edgeService;
    private final ProcessVersionService versionService;
    private final ProcessNodeDefinitionService definitions;
    private final AiProcessConfigurationService configuration;
    private final AiProcessOptionsService options;
    private final AiProcessFormService forms;
    private final JsonMapper mapper;
    private final EntityManager entityManager;
    private final Validator validator;
    private final ScopedAuditService audit;

    public AiChatProcessService(@Nonnull PermissionService permissions, @Nonnull AiChatSessionRepository sessions, @Nonnull UserService users,
                               @Nonnull ProcessVersionRepository versions, @Nonnull ProcessNodeRepository nodes, @Nonnull ProcessEdgeRepository edges,
                               @Nonnull ProcessNodeService nodeService, @Nonnull ProcessEdgeService edgeService, @Nonnull ProcessVersionService versionService,
                               @Nonnull ProcessNodeDefinitionService definitions, @Nonnull AiProcessConfigurationService configuration,
                               @Nonnull AiProcessOptionsService options, @Nonnull AiProcessFormService forms, @Nonnull JsonMapper mapper,
                               @Nonnull EntityManager entityManager, @Nonnull Validator validator, @Nonnull AuditService audit) {
        this.permissions = permissions;
        this.sessions = sessions;
        this.users = users;
        this.versions = versions;
        this.nodes = nodes;
        this.edges = edges;
        this.nodeService = nodeService;
        this.edgeService = edgeService;
        this.versionService = versionService;
        this.definitions = definitions;
        this.configuration = configuration;
        this.options = options;
        this.forms = forms;
        this.mapper = mapper;
        this.entityManager = entityManager;
        this.validator = validator;
        this.audit = audit.createScopedAuditService(AiChatProcessService.class, "Prozesse");
    }

    @Nonnull
    public ProcessVersionEntity requireContext(@Nonnull AiProcessChatContext context) throws ResponseException {
        return scope(context, false);
    }

    private ProcessVersionEntity scope(AiProcessChatContext context, boolean write) throws ResponseException {
        permissions.requireSystemPermission(context.userId(), AiChatPermissionProvider.AI_CHAT_USE);
        if (sessions.findByUserIdAndSessionId(context.userId(), context.sessionId()).isEmpty()) throw ResponseException.notFound();
        permissions.requireProcessPermission(context.userId(), context.processId(), ProcessPermissionProvider.PROCESS_DEFINITION_READ);
        if (write) permissions.requireProcessPermission(context.userId(), context.processId(), ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE);
        var version = versions.findById(ProcessVersionEntityId.of(context.processId(), context.processVersion())).orElseThrow(ResponseException::notFound);
        if (write) {
            // Serialize graph mutations in a version, then refresh/lock the affected rows before applying patches.
            entityManager.refresh(version, LockModeType.PESSIMISTIC_WRITE);
            if (version.getStatus() != ProcessVersionStatus.Drafted) throw ResponseException.badRequest("Nur Prozessversionen im Entwurfsstatus können bearbeitet werden.");
        }
        return version;
    }

    private UserEntity user(AiProcessChatContext context) throws ResponseException {
        return users.retrieve(context.userId()).orElseThrow(ResponseException::unauthorized);
    }

    private ProcessNodeEntity node(AiProcessChatContext context, int id, boolean write) throws ResponseException {
        var node = nodes.findById(id).filter(n -> n.getProcessId().equals(context.processId()) && n.getProcessVersion().equals(context.processVersion())).orElseThrow(ResponseException::notFound);
        if (write) entityManager.refresh(node, LockModeType.PESSIMISTIC_WRITE);
        return node;
    }

    private ProcessEdgeEntity edge(AiProcessChatContext context, int id, boolean write) throws ResponseException {
        var edge = edges.findById(id).filter(e -> e.getProcessId().equals(context.processId()) && e.getProcessVersion().equals(context.processVersion())).orElseThrow(ResponseException::notFound);
        if (write) entityManager.refresh(edge, LockModeType.PESSIMISTIC_WRITE);
        return edge;
    }

    @Nonnull
    public Object listDefinitions(@Nonnull AiProcessChatContext context, @Nullable String query, @Nullable Integer offset, @Nullable Integer limit) throws ResponseException {
        scope(context, false);
        return AiToolResults.page(definitions.getAllProcessNodeDefinitions().stream()
                .sorted(Comparator.comparing(ProcessNodeDefinition::getKey))
                .filter(d -> AiToolResults.matches(query, d.getKey() + " " + d.getName() + " " + d.getAbstract()))
                .map(this::definitionSummary).toList(), offset, limit);
    }

    private Map<String, Object> definitionSummary(ProcessNodeDefinition<?> definition) {
        return Map.of("key", definition.getKey(), "version", definition.getMajorVersion(), "name", definition.getName(),
                "type", definition.getType(), "summary", Objects.requireNonNullElse(definition.getAbstract(), ""));
    }

    @Nonnull
    public Object definition(@Nonnull AiProcessChatContext context, @Nonnull String key, int version, int descriptionOffset) throws ResponseException {
        scope(context, false);
        var definition = definitions.getProcessNodeDefinition(key, version).orElseThrow(ResponseException::notFound);
        var result = new LinkedHashMap<>(definitionSummary(definition));
        result.put("description", AiToolResults.value(mapper, definition.getDescription(), descriptionOffset, 3000));
        result.put("ports", definition.getPorts());
        result.put("outputs", definition.getOutputs());
        result.put("executionTypes", definition.getExecutionTypes());
        return result;
    }

    @Nonnull
    public Object structure(@Nonnull AiProcessChatContext context, @Nullable Integer offset, @Nullable Integer limit) throws ResponseException {
        var version = scope(context, false);
        return Map.of("processId", context.processId(), "processVersion", context.processVersion(), "status", version.getStatus(),
                "nodes", AiToolResults.page(nodes.findAllByProcessIdAndProcessVersion(context.processId(), context.processVersion()).stream()
                        .sorted(Comparator.comparing(ProcessNodeEntity::getId)).map(this::nodeSummary).toList(), offset, limit),
                "edges", AiToolResults.page(edges.findAllByProcessIdAndProcessVersion(context.processId(), context.processVersion()).stream()
                        .sorted(Comparator.comparing(ProcessEdgeEntity::getId)).map(this::edgeSummary).toList(), offset, limit));
    }

    private Map<String, Object> nodeSummary(ProcessNodeEntity node) {
        return Map.of("id", node.getId(), "name", Objects.requireNonNullElse(node.getName(), ""), "dataKey", node.getDataKey(),
                "definitionKey", node.getProcessNodeDefinitionKey(), "definitionVersion", node.getProcessNodeDefinitionVersion(), "savedWithErrors", node.getSavedWithErrors());
    }

    private Map<String, Object> edgeSummary(ProcessEdgeEntity edge) {
        return Map.of("id", edge.getId(), "fromNodeId", edge.getFromNodeId(), "toNodeId", edge.getToNodeId(), "viaPort", edge.getViaPort());
    }

    @Nonnull
    public Object getNode(@Nonnull AiProcessChatContext context, int id) throws ResponseException {
        return getNode(context, id, null, 0);
    }

    @Nonnull
    public Object getNode(@Nonnull AiProcessChatContext context, int id, @Nullable String property, int valueOffset) throws ResponseException {
        scope(context, false);
        var node = node(context, id, false);
        if (property != null) {
            if (!EDITABLE.contains(property)) throw ResponseException.badRequest("Diese Eigenschaft ist nicht verfügbar. Konfigurationswerte über die Feld-Tools abrufen.");
            return AiToolResults.value(mapper, mapper.valueToTree(node).get(property), valueOffset, 4000);
        }
        var result = new LinkedHashMap<>(nodeSummary(node));
        var tree = mapper.valueToTree(node);
        for (var key : EDITABLE) result.put(key, AiToolResults.value(mapper, tree.get(key), 0, 500));
        result.put("configurationFields", node.getConfiguration().keySet());
        return result;
    }

    @Nonnull
    public Object fields(@Nonnull AiProcessChatContext context, int id, @Nullable String query, @Nullable Integer offset, @Nullable Integer limit) throws ResponseException {
        scope(context, false);
        var node = node(context, id, false);
        return AiToolResults.page(configuration.fields(node, user(context)).stream()
                .filter(f -> AiToolResults.matches(query, f.element().getLabel() + " " + f.valuePath()))
                .map(f -> configuration.summary(f, node)).toList(), offset, limit);
    }

    @Nonnull
    public Object field(@Nonnull AiProcessChatContext context, int id, @Nonnull String path, int valueOffset, int constraintsOffset) throws ResponseException {
        scope(context, false);
        var node = node(context, id, false);
        return configuration.details(configuration.field(node, user(context), path), node, valueOffset, constraintsOffset);
    }

    @Nonnull
    public Object options(@Nonnull AiProcessChatContext context, int id, @Nonnull String path, @Nullable String query, @Nullable Integer offset, @Nullable Integer limit) throws ResponseException {
        scope(context, false);
        var node = node(context, id, false);
        return options.options(context, node, configuration.field(node, user(context), path).element(), query, offset, limit);
    }

    @Nonnull
    public Object variables(@Nonnull AiProcessChatContext context, int id, @Nullable String query, @Nullable Integer offset, @Nullable Integer limit) throws ResponseException {
        scope(context, false);
        var metadata = nodeService.getIncomingProcessNodeDefinitionMetadata(node(context, id, false), user(context));
        return AiToolResults.page(metadata.inputVariables().stream().filter(v -> AiToolResults.matches(query, v.label() + " " + v.path()))
                .map(v -> {
                    var result = new LinkedHashMap<String, Object>();
                    result.put("source", v.source()); result.put("path", v.path()); result.put("nodeDataKey", v.nodeDataKey());
                    result.put("label", v.label()); result.put("description", v.description());
                    return result;
                }).toList(), offset, limit);
    }

    @Nonnull
    public Object help(@Nonnull AiProcessChatContext context, @Nonnull String topic, @Nullable String key, @Nullable Integer offset, @Nullable Integer limit) throws ResponseException {
        scope(context, false);
        return options.help(topic, key, offset, limit);
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public Object createNode(@Nonnull AiProcessChatContext context, @Nonnull String key, int version, @Nonnull String dataKey, @Nullable String name) throws ResponseException {
        scope(context, true);
        var actor = user(context);
        requireUniqueDataKey(context, null, dataKey);
        var node = new ProcessNodeEntity().setProcessId(context.processId()).setProcessVersion(context.processVersion())
                .setProcessNodeDefinitionKey(key).setProcessNodeDefinitionVersion(version).setDataKey(dataKey).setName(name)
                .setConfiguration(new AuthoredElementValues()).setOutputMappings(Map.of());
        validateEntity(node);
        var saved = nodeService.create(node);
        var problems = nodeService.validate(saved, definitions.getProcessNodeDefinition(saved).orElseThrow(ResponseException::badRequest), false, actor);
        saved.setSavedWithErrors(problems.isPresent());
        validateEntity(saved);
        nodes.saveAndFlush(saved);
        log(actor, AuditAction.Create, saved, null, snapshot(saved));
        return mutation(saved, problems.map(p -> p.problems()).orElse(List.of()));
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public Object updateNode(@Nonnull AiProcessChatContext context, int id, @Nonnull Map<String, Object> properties,
                             @Nonnull List<AiProcessConfigurationChange> configurationChanges,
                             @Nonnull List<String> removals) throws ResponseException {
        scope(context, true);
        var existing = node(context, id, true);
        var actor = user(context);
        var before = snapshot(existing);
        if (!EDITABLE.containsAll(properties.keySet())) throw ResponseException.badRequest("Die Knoteneigenschaften enthalten ein nicht bearbeitbares Feld.");
        var tree = (ObjectNode) mapper.valueToTree(existing);
        properties.forEach((key, value) -> tree.set(key, mapper.valueToTree(value)));
        var candidate = mapper.treeToValue(tree, ProcessNodeEntity.class);
        var patch = configuration.patch(candidate, actor, configurationChanges, removals);
        if (!patch.valid()) {
            return Map.of("id", id, "saved", false, "errors", patch.errors());
        }
        candidate.setConfiguration(patch.configuration());
        return save(context, actor, candidate, existing, before);
    }

    private Object save(AiProcessChatContext context, UserEntity actor, ProcessNodeEntity candidate, ProcessNodeEntity existing, Map<String, Object> before) throws ResponseException {
        requireUniqueDataKey(context, candidate.getId(), candidate.getDataKey());
        validateEntity(candidate);
        var saved = nodeService.updateForAuthoring(candidate.getId(), candidate, existing, actor);
        nodes.flush();
        var problems = nodeService.validate(saved, definitions.getProcessNodeDefinition(saved).orElseThrow(ResponseException::badRequest), false, actor);
        log(actor, AuditAction.Update, saved, before, snapshot(saved));
        return mutation(saved, problems.map(p -> p.problems()).orElse(List.of()));
    }

    private Object mutation(ProcessNodeEntity saved, List<String> problems) {
        return Map.of("id", saved.getId(), "saved", true, "savedWithErrors", saved.getSavedWithErrors(), "problems", problemSummary(problems));
    }

    private void requireUniqueDataKey(AiProcessChatContext context, Integer id, String dataKey) throws ResponseException {
        if (dataKey == null || dataKey.isBlank() || dataKey.length() > 32) throw ResponseException.badRequest("Der Datenschlüssel muss zwischen 1 und 32 Zeichen lang sein.");
        if (nodes.findAllByProcessIdAndProcessVersion(context.processId(), context.processVersion()).stream()
                .anyMatch(n -> !Objects.equals(n.getId(), id) && n.getDataKey().equals(dataKey))) throw ResponseException.badRequest("Der Datenschlüssel wird in dieser Prozessversion bereits verwendet.");
    }

    private void validateEntity(Object entity) throws ResponseException {
        var errors = validator.validate(entity);
        if (!errors.isEmpty()) throw ResponseException.badRequest(errors.stream().map(v -> v.getMessage()).sorted().findFirst().orElseThrow());
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public Object deleteNode(@Nonnull AiProcessChatContext context, int id) throws ResponseException {
        scope(context, true);
        var node = node(context, id, true);
        var actor = user(context);
        var before = snapshot(node);
        var connected = edges.findAllByProcessIdAndProcessVersion(context.processId(), context.processVersion()).stream()
                .filter(e -> e.getFromNodeId().equals(id) || e.getToNodeId().equals(id)).toList();
        for (var connection : connected) {
            edge(context, connection.getId(), true);
            edgeService.performDelete(connection);
        }
        nodeService.performDelete(node);
        nodes.flush();
        before.put("deletedEdges", connected.stream().map(this::edgeSummary).toList());
        log(actor, AuditAction.Delete, node, before, null);
        return Map.of("deletedNodeId", id, "deletedEdgeIds", connected.stream().map(ProcessEdgeEntity::getId).toList(), "saved", true);
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public Object saveEdge(@Nonnull AiProcessChatContext context, @Nullable Integer id, int fromNodeId, int toNodeId, @Nonnull String port) throws ResponseException {
        scope(context, true);
        var actor = user(context);
        var source = node(context, fromNodeId, true);
        node(context, toNodeId, true);
        var provider = definitions.getProcessNodeDefinition(source).orElseThrow(ResponseException::badRequest);
        if (provider.getPorts().stream().noneMatch(p -> p.key().equals(port))) throw ResponseException.badRequest("Der Ausgang ist für diesen Knoten nicht definiert.");
        if (edges.findByFromNodeIdAndViaPort(fromNodeId, port).filter(e -> !Objects.equals(e.getId(), id)).isPresent()) throw ResponseException.badRequest("Der Ausgang ist bereits verbunden. Ändern oder löschen Sie die vorhandene Verbindung über ihre ID.");
        var existing = id == null ? null : edge(context, id, true);
        var before = existing == null ? null : snapshot(existing);
        var candidate = new ProcessEdgeEntity().setProcessId(context.processId()).setProcessVersion(context.processVersion())
                .setFromNodeId(fromNodeId).setToNodeId(toNodeId).setViaPort(port);
        if (id != null) candidate.setId(id);
        validateEntity(candidate);
        var saved = existing == null ? edgeService.create(candidate) : edgeService.performUpdate(id, candidate, existing);
        validateEntity(saved);
        edges.flush();
        log(actor, existing == null ? AuditAction.Create : AuditAction.Update, saved, before, snapshot(saved));
        return Map.of("saved", true, "edge", edgeSummary(saved));
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public Object deleteEdge(@Nonnull AiProcessChatContext context, int id) throws ResponseException {
        scope(context, true);
        var edge = edge(context, id, true);
        var actor = user(context);
        var before = snapshot(edge);
        edgeService.performDelete(edge);
        edges.flush();
        log(actor, AuditAction.Delete, edge, before, null);
        return Map.of("deletedEdgeId", id, "saved", true);
    }

    @Nonnull
    public Object form(@Nonnull AiProcessChatContext context, int id, @Nonnull String valuePath, @Nullable String elementPath, @Nullable Integer offset, @Nullable Integer limit) throws ResponseException {
        return form(context, id, valuePath, elementPath, null, 0, offset, limit);
    }

    @Nonnull
    public Object form(@Nonnull AiProcessChatContext context, int id, @Nonnull String valuePath, @Nullable String elementPath,
                       @Nullable String property, int valueOffset, @Nullable Integer offset, @Nullable Integer limit) throws ResponseException {
        scope(context, false);
        var node = node(context, id, false);
        return forms.read(node, configuration.field(node, user(context), valuePath), elementPath, property, valueOffset, offset, limit);
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public Object editForm(@Nonnull AiProcessChatContext context, int id, @Nonnull String valuePath, @Nonnull String operation,
                           @Nonnull String elementPath, @Nullable String parentPath, @Nullable Integer type,
                           @Nonnull Map<String, Object> properties) throws ResponseException {
        scope(context, true);
        var existing = node(context, id, true);
        var actor = user(context);
        var before = snapshot(existing);
        var candidate = mapper.convertValue(existing, ProcessNodeEntity.class);
        var edited = forms.edit(candidate, configuration.field(candidate, actor, valuePath), operation, elementPath, parentPath, type, properties);
        var result = save(context, actor, candidate, existing, before);
        return Map.of("node", result, "element", edited);
    }

    @Nonnull
    public Object validate(@Nonnull AiProcessChatContext context, @Nullable Integer offset, @Nullable Integer limit) throws ResponseException {
        var version = scope(context, false);
        var result = versionService.validate(version, user(context));
        return Map.of("valid", !result.hasAnyProblems(), "versionProblems", result.versionProblems(),
                "nodeProblems", AiToolResults.page(result.nodeProblems().stream().map(p -> Map.of("nodeId", p.node().getId(),
                        "problems", problemSummary(p.problems()))).toList(), offset, limit));
    }

    private Map<String, Object> problemSummary(List<String> problems) {
        return Map.of("messages", problems.stream().limit(20).toList(), "additionalCount", Math.max(0, problems.size() - 20));
    }

    private Map<String, Object> snapshot(Object entity) {
        return mapper.convertValue(entity, new TypeReference<LinkedHashMap<String, Object>>() {});
    }

    private void log(UserEntity actor, AuditAction action, Object entity, @Nullable Map<String, Object> before, @Nullable Map<String, Object> after) {
        Object id = entity instanceof ProcessNodeEntity node ? node.getId() : ((ProcessEdgeEntity) entity).getId();
        audit.create().withUser(actor).withAuditAction(action, entity.getClass(), id, "id")
                .withDiff(before, after).withMessage("Prozess über den KI-Chat bearbeitet.").log();
    }
}
