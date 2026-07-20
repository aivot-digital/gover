package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.elements.models.ElementDerivationOptions;
import de.aivot.gover.backend.elements.models.ElementDerivationRequest;
import de.aivot.gover.backend.elements.models.elements.BaseInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.gover.backend.elements.services.ElementDerivationService;
import de.aivot.gover.backend.elements.utils.ElementPOJOMapper;
import de.aivot.gover.backend.elements.utils.ElementStreamUtils;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.lib.services.EntityService;
import de.aivot.gover.backend.models.config.GoverConfig;
import de.aivot.gover.backend.plugins.form.FormPlugin;
import de.aivot.gover.backend.process.entities.ProcessEdgeEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntityId;
import de.aivot.gover.backend.process.enums.ProcessNodeType;
import de.aivot.gover.backend.process.filters.ProcessNodeFilter;
import de.aivot.gover.backend.process.models.ProcessNodeDefinition;
import de.aivot.gover.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.gover.backend.process.models.ProcessNodeProblems;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.gover.backend.process.repositories.ProcessEdgeRepository;
import de.aivot.gover.backend.process.repositories.ProcessNodeRepository;
import de.aivot.gover.backend.process.repositories.ProcessRepository;
import de.aivot.gover.backend.process.repositories.ProcessVersionRepository;
import de.aivot.gover.backend.user.entities.UserEntity;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProcessNodeService implements EntityService<ProcessNodeEntity, Integer> {

    private final ProcessNodeRepository processNodeRepository;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ElementDerivationService elementDerivationService;
    private final UserService userService;
    private final ProcessRepository processDefinitionRepository;
    private final ProcessVersionRepository processDefinitionVersionRepository;
    private final ProcessEdgeRepository processEdgeRepository;
    private final GoverConfig goverConfig;

    @Autowired
    public ProcessNodeService(ProcessNodeRepository processNodeRepository,
                              ProcessNodeDefinitionService processNodeProviderService,
                              ElementDerivationService elementDerivationService,
                              UserService userService,
                              ProcessRepository processDefinitionRepository,
                              ProcessVersionRepository processDefinitionVersionRepository,
                              ProcessEdgeRepository processEdgeRepository,
                              GoverConfig goverConfig) {
        this.processNodeRepository = processNodeRepository;
        this.processNodeProviderService = processNodeProviderService;
        this.elementDerivationService = elementDerivationService;
        this.userService = userService;
        this.processDefinitionRepository = processDefinitionRepository;
        this.processDefinitionVersionRepository = processDefinitionVersionRepository;
        this.processEdgeRepository = processEdgeRepository;
        this.goverConfig = goverConfig;
    }

    @Nonnull
    @Override
    public ProcessNodeEntity create(@Nonnull ProcessNodeEntity entity) throws ResponseException {
        // No element derivation and configuration check needs to be done here.
        // The initial create of a process node can be done without configuration checking.
        // This allows us to create nodes without needing to provide a default, fully valid configuration.
        // The validity of the configuration will be checked at least before the publishing of the process version.

        // Set the ID to null, to force the database to assign a new, valid ID.
        entity.setId(null);

        // Check if the referenced process node provider exists.
        var provider = processNodeProviderService
                .getProcessNodeDefinition(entity.getProcessNodeDefinitionKey(), entity.getProcessNodeDefinitionVersion())
                .orElseThrow(() -> ResponseException.badRequest(
                        "Der Prozesselement-Funktionsanbieter %s (Version %s) existiert nicht.",
                        StringUtils.quote(entity.getProcessNodeDefinitionKey()),
                        entity.getProcessNodeDefinitionVersion()
                ));

        validateProcessNodeDefinitionUsable(provider);
        validateProcessNodeTypeCapacity(entity, provider, null);

        if (entity.getName() == null || StringUtils.isNullOrEmpty(entity.getName())) {
            entity.setName(provider.getName());
        }

        // Save the process node.
        return processNodeRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessNodeEntity> performList(@Nonnull Pageable pageable,
                                               @Nullable Specification<ProcessNodeEntity> specification,
                                               @Nullable Filter<ProcessNodeEntity> filter) throws ResponseException {
        return processNodeRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return processNodeRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeEntity> retrieve(@Nonnull Specification<ProcessNodeEntity> specification) throws ResponseException {
        return processNodeRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return processNodeRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessNodeEntity> specification) {
        return processNodeRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessNodeEntity performUpdate(@Nonnull Integer id,
                                           @Nonnull ProcessNodeEntity entity,
                                           @Nonnull ProcessNodeEntity existingEntity) throws ResponseException {
        // Update fields
        existingEntity.setProcessId(entity.getProcessId());
        existingEntity.setProcessVersion(entity.getProcessVersion());
        existingEntity.setName(entity.getName());
        existingEntity.setDescription(entity.getDescription());
        existingEntity.setDataKey(entity.getDataKey());
        existingEntity.setProcessNodeDefinitionKey(entity.getProcessNodeDefinitionKey());
        existingEntity.setProcessNodeDefinitionVersion(entity.getProcessNodeDefinitionVersion());
        existingEntity.setOutputMappings(entity.getOutputMappings());
        existingEntity.setTimeLimitDays(entity.getTimeLimitDays());
        existingEntity.setNotes(entity.getNotes());
        existingEntity.setRequirements(entity.getRequirements());
        existingEntity.setConfiguration(entity.getConfiguration());

        var provider = processNodeProviderService
                .getProcessNodeDefinition(existingEntity)
                .orElseThrow(ResponseException::badRequest);

        validateProcessNodeDefinitionUsable(provider);
        validateProcessNodeTypeCapacity(existingEntity, provider, existingEntity.getId());

        if (existingEntity.getName() == null || StringUtils.isNullOrEmpty(existingEntity.getName())) {
            existingEntity.setName(provider.getName());
        }

        // Validate the node configuration
        validate(existingEntity, provider, false).ifPresentOrElse(
                (ignored) -> {
                    existingEntity.setSavedWithErrors(true);
                },
                () -> {
                    existingEntity.setSavedWithErrors(false);
                }
        );

        return processNodeRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessNodeEntity entity) throws ResponseException {
        processNodeRepository.delete(entity);
    }

    @Nonnull
    public <NodeConfig> ProcessConfigurationDetails<NodeConfig> deriveConfiguration(@Nonnull ProcessNodeEntity entity,
                                                                                    @Nonnull ProcessNodeDefinition<NodeConfig> provider,
                                                                                    @Nullable UserEntity user,
                                                                                    @Nonnull Boolean skipErrors) throws ResponseException {
        var layout = getConfigLayoutElement(entity, provider, user);

        var edo = new ElementDerivationOptions();

        if (skipErrors) {
            edo.setSkipErrorsForElementIds(List.of(ElementDerivationOptions.ALL_ELEMENTS));
        }

        var edr = new ElementDerivationRequest(
                layout,
                entity.getConfiguration(),
                edo
        );
        var derivedData = elementDerivationService.derive(edr);

        NodeConfig config;
        try {
            config = ElementPOJOMapper.mapToPOJO(derivedData.getEffectiveValues(), provider.getNodeConfigurationClass());
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Die Ableitung der Knotenkonfiguration ist fehlgeschlagen: %s", e.getMessage());
        }

        return new ProcessConfigurationDetails<NodeConfig>(
                config,
                derivedData
        );
    }

    @Nonnull
    private <NodeConfig> ConfigLayoutElement getConfigLayoutElement(@Nonnull ProcessNodeEntity entity, @Nonnull ProcessNodeDefinition<NodeConfig> provider, @Nullable UserEntity user) throws ResponseException {
        if (user == null &&
                SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof Jwt jwt) {
            user = userService
                    .fromJWT(jwt)
                    .orElseThrow(ResponseException::unauthorized);
        }

        var processDefinition = processDefinitionRepository
                .findById(entity.getProcessId())
                .orElseThrow(ResponseException::badRequest);

        var processVersion = processDefinitionVersionRepository
                .findById(ProcessVersionEntityId.of(processDefinition.getId(), entity.getProcessVersion()))
                .orElseThrow(ResponseException::badRequest);

        var context = new ProcessNodeDefinitionConfigurationLayoutContext(
                user,
                processDefinition,
                processVersion,
                entity
        );

        return provider
                .getConfigurationLayout(context);
    }

    public Set<String> getAllUsedDataKeys(@Nonnull Integer processId, @Nonnull Integer processVersion) {
        return processNodeRepository.findAllDataKeysByProcessIdAndVersion(processId, processVersion);
    }

    public List<ProcessNodeEntity> findAllByProcessIdAndProcessVersion(Integer processId, Integer processVersion) {
        return processNodeRepository
                .findAllByProcessIdAndProcessVersion(processId, processVersion);
    }

    /**
     * Validates nodes before a whole process version is copied into an empty target version. This preflight keeps
     * imports and draft creation from leaving behind a process shell when the module or limit policy rejects a later
     * node in the batch.
     */
    public void validateNewProcessNodeBatch(@Nonnull List<ProcessNodeEntity> nodes) throws ResponseException {
        var countsByType = new EnumMap<ProcessNodeType, Integer>(ProcessNodeType.class);

        for (var node : nodes) {
            var provider = processNodeProviderService
                    .getProcessNodeDefinition(node)
                    .orElseThrow(() -> ResponseException.badRequest(
                            "Eine Prozesselementdefinition mit dem Schlüssel „%s“ und der Version „%d“ ist nicht verfügbar."
                                    .formatted(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())
                    ));

            validateProcessNodeDefinitionUsable(provider);
            countsByType.merge(provider.getType(), 1, Integer::sum);
        }

        for (var entry : countsByType.entrySet()) {
            validateProcessNodeTypeBatchCapacity(entry.getKey(), entry.getValue());
        }
    }

    @Nonnull
    public ProcessNodeDefinitionMetadata getProcessDataKeyHintResponses(@Nonnull ProcessNodeEntity node) throws ResponseException {
        var processNodesById = new LinkedHashMap<Integer, ProcessNodeEntity>();

        processNodeRepository
                .findAllByProcessIdAndProcessVersion(node.getProcessId(), node.getProcessVersion())
                .stream()
                .sorted(Comparator.comparing(ProcessNodeEntity::getId))
                .forEach(processNode -> processNodesById.put(processNode.getId(), processNode));

        var incomingEdgesByNodeId = buildIncomingEdgesByNodeId(
                processEdgeRepository
                        .findAllByProcessIdAndProcessVersion(node.getProcessId(), node.getProcessVersion())
        );

        var previousNodes = new ArrayList<ProcessNodeEntity>();
        collectPreviousNodes(
                node.getId(),
                node.getId(),
                incomingEdgesByNodeId,
                processNodesById,
                new HashSet<>(),
                new HashSet<>(),
                previousNodes
        );

        var previousMetadata = ProcessNodeDefinitionMetadata
                .empty();

        for (var previousNode : previousNodes) {
            previousMetadata = calculateProcessDataKeyHintsForNode(previousNode, previousMetadata);
        }

        return previousMetadata;
    }

    @Nonnull
    private Map<Integer, List<ProcessEdgeEntity>> buildIncomingEdgesByNodeId(@Nonnull List<ProcessEdgeEntity> edges) {
        var incomingEdgesByNodeId = new HashMap<Integer, List<ProcessEdgeEntity>>();

        edges.stream()
                .sorted(Comparator.comparing(ProcessEdgeEntity::getId))
                .forEach(edge -> incomingEdgesByNodeId
                        .computeIfAbsent(edge.getToNodeId(), ignored -> new ArrayList<>())
                        .add(edge));

        return incomingEdgesByNodeId;
    }

    private void collectPreviousNodes(@Nonnull Integer currentNodeId,
                                      @Nonnull Integer targetNodeId,
                                      @Nonnull Map<Integer, List<ProcessEdgeEntity>> incomingEdgesByNodeId,
                                      @Nonnull Map<Integer, ProcessNodeEntity> processNodesById,
                                      @Nonnull Set<Integer> visitedNodeIds,
                                      @Nonnull Set<Integer> traversalStackNodeIds,
                                      @Nonnull List<ProcessNodeEntity> previousNodes) {
        var incomingEdges = incomingEdgesByNodeId.getOrDefault(currentNodeId, List.of());

        for (var incomingEdge : incomingEdges) {
            var previousNodeId = incomingEdge.getFromNodeId();

            if (previousNodeId.equals(targetNodeId)) {
                continue;
            }

            collectPreviousNode(
                    previousNodeId,
                    targetNodeId,
                    incomingEdgesByNodeId,
                    processNodesById,
                    visitedNodeIds,
                    traversalStackNodeIds,
                    previousNodes
            );
        }
    }

    private void collectPreviousNode(@Nonnull Integer currentNodeId,
                                     @Nonnull Integer targetNodeId,
                                     @Nonnull Map<Integer, List<ProcessEdgeEntity>> incomingEdgesByNodeId,
                                     @Nonnull Map<Integer, ProcessNodeEntity> processNodesById,
                                     @Nonnull Set<Integer> visitedNodeIds,
                                     @Nonnull Set<Integer> traversalStackNodeIds,
                                     @Nonnull List<ProcessNodeEntity> previousNodes) {
        if (currentNodeId.equals(targetNodeId) ||
                visitedNodeIds.contains(currentNodeId) ||
                !traversalStackNodeIds.add(currentNodeId)) {
            return;
        }

        collectPreviousNodes(
                currentNodeId,
                targetNodeId,
                incomingEdgesByNodeId,
                processNodesById,
                visitedNodeIds,
                traversalStackNodeIds,
                previousNodes
        );

        traversalStackNodeIds.remove(currentNodeId);
        visitedNodeIds.add(currentNodeId);

        var currentNode = processNodesById.get(currentNodeId);
        if (currentNode != null) {
            previousNodes.add(currentNode);
        }
    }

    @Nonnull
    private ProcessNodeDefinitionMetadata calculateProcessDataKeyHintsForNode(@Nonnull ProcessNodeEntity node,
                                                                              @Nonnull ProcessNodeDefinitionMetadata previousMetadata) throws ResponseException {

        var provider = processNodeProviderService
                .getProcessNodeDefinition(node)
                .orElseThrow(ResponseException::badRequest);

        return calculateProcessDataKeyHintsForNode(node, provider, previousMetadata);
    }

    @Nonnull
    private <NodeConfig> ProcessNodeDefinitionMetadata calculateProcessDataKeyHintsForNode(@Nonnull ProcessNodeEntity node,
                                                                                           @Nonnull ProcessNodeDefinition<NodeConfig> provider,
                                                                                           @Nonnull ProcessNodeDefinitionMetadata previousMetadata) throws ResponseException {
        var configuration = deriveConfiguration(node, provider, null, true);
        var currentMetadata = provider.getMetadata(
                node,
                configuration.configuration(),
                previousMetadata
        );

        for (var output : provider.getOutputs()) {
            var mappedProcessDataKey = StringUtils.toNullableTrimmedString(node.getOutputMappings().get(output.key()));
            if (mappedProcessDataKey == null) {
                continue;
            }

            currentMetadata.addForwardedProcessDataKey(
                    mappedProcessDataKey,
                    output.label(),
                    output.description(),
                    node
            );
        }

        return currentMetadata;
    }

    @Nonnull
    public <NodeConfig> Optional<ProcessNodeProblems> validate(@Nonnull ProcessNodeEntity node,
                                                               @Nonnull ProcessNodeDefinition<NodeConfig> provider,
                                                               @Nonnull Boolean checkPorts) throws ResponseException {
        var commonErrors = new LinkedHashMap<String, List<String>>();
        var problems = new LinkedList<String>();

        collectProcessNodePolicyProblems(node, provider, problems);

        if (StringUtils.isNullOrEmpty(node.getDataKey())) {
            var commonErrorMessage = "Der Datenschlüssel darf nicht leer sein.";
            addCommonError(
                    commonErrors,
                    problems,
                    ProcessNodeProblems.COMMON_ERROR_KEY_DATA_KEY,
                    "Datenschlüssel",
                    commonErrorMessage
            );
        } else {
            var duplicateDataKeyFilter = ProcessNodeFilter
                    .create()
                    .setNotId(node.getId())
                    .setDataKey(node.getDataKey())
                    .setProcessId(node.getProcessId())
                    .setProcessVersion(node.getProcessVersion());

            if (this.exists(duplicateDataKeyFilter)) {
                var commonErrorMessage = "Es existiert mindestens ein weiterer Knoten mit dem selben Datenschlüssel. Datenschlüssel müssen eindeutig sein.";
                addCommonError(
                        commonErrors,
                        problems,
                        ProcessNodeProblems.COMMON_ERROR_KEY_DATA_KEY,
                        "Datenschlüssel",
                        commonErrorMessage
                );
            }
        }

        var layout = getConfigLayoutElement(node, provider, null);

        ProcessConfigurationDetails<NodeConfig> derivedConfiguration;
        try {
            derivedConfiguration = this
                    .deriveConfiguration(node, provider, null, false);
        } catch (ResponseException e) {
            problems.add(e.getMessage());
            derivedConfiguration = null;
        }

        if (derivedConfiguration != null) {
            ElementStreamUtils.applyAction(
                    layout,
                    derivedConfiguration.derivedRuntimeElementData.getElementStates(),
                    (e, state) -> {
                        if (e instanceof BaseInputElement<?> input) {
                            if (StringUtils.isNotNullOrEmpty(state.getError())) {
                                problems.add(input.getLabel() + ": " + state.getError());
                            }
                        }
                    }
            );

            var validationErrors = provider
                    .validateConfiguration(node, derivedConfiguration.configuration);

            if (validationErrors != null) {
                for (var err : validationErrors.entrySet()) {
                    // Mirror provider validation errors into element states so the editor can mark the field itself.
                    var combinedValidationError = combineValidationErrors(err.getValue());
                    if (StringUtils.isNotNullOrEmpty(combinedValidationError)) {
                        derivedConfiguration.derivedRuntimeElementData.putError(
                                err.getKey(),
                                combinedValidationError
                        );
                    }

                    for (var validationError : err.getValue()) {
                        layout.findChild(err.getKey(), BaseInputElement.class).ifPresentOrElse(
                                element -> problems.add(element.getLabel() + ": " + validationError),
                                () -> problems.add("Element mit ID " + err.getKey() + ": " + validationError)
                        );
                    }
                }
            }
        }

        if (checkPorts) {
            for (var ports : provider.getPorts()) {
                var edgeExists = processEdgeRepository
                        .existsByFromNodeIdAndViaPort(node.getId(), ports.key());

                if (!edgeExists) {
                    problems.add(
                            "Es existiert keine Verbindung von diesem Knoten über den Ausgang " +
                                    StringUtils.quote(ports.label()) +
                                    ". Alle Ausgänge müssen mit einer Verbindung zu einem anderen Knoten verbunden sein."
                    );
                }
            }
        }

        if (problems.isEmpty()) {
            return Optional.empty();
        } else {

            return Optional.of(new ProcessNodeProblems(node, problems, commonErrors,
                    derivedConfiguration != null ? derivedConfiguration.derivedRuntimeElementData : new DerivedRuntimeElementData()));
        }
    }

    @Nonnull
    private static String combineValidationErrors(@Nonnull List<String> validationErrors) {
        var cleanedErrors = validationErrors
                .stream()
                .filter(StringUtils::isNotNullOrEmpty)
                .distinct()
                .toList();

        return String.join(" ", cleanedErrors);
    }

    private void collectProcessNodePolicyProblems(@Nonnull ProcessNodeEntity node,
                                                  @Nonnull ProcessNodeDefinition<?> provider,
                                                  @Nonnull List<String> problems) throws ResponseException {
        if (isProcessNodeDefinitionDisabled(provider)) {
            problems.add(createDisabledFormModuleMessage());
        }

        var type = provider.getType();
        if (goverConfig.isProcessNodeTypeUnlimited(type)) {
            return;
        }

        var limit = goverConfig.getProcessNodeLimit(type);
        var count = countProcessNodesOfType(
                node.getProcessId(),
                node.getProcessVersion(),
                type,
                null
        );

        if (count > limit) {
            problems.add(createProcessNodeLimitExceededMessage(type, limit));
        }
    }

    private void validateProcessNodeDefinitionUsable(@Nonnull ProcessNodeDefinition<?> provider) throws ResponseException {
        if (isProcessNodeDefinitionDisabled(provider)) {
            throw ResponseException.badRequest(createDisabledFormModuleMessage());
        }
    }

    private boolean isProcessNodeDefinitionDisabled(@Nonnull ProcessNodeDefinition<?> provider) {
        return FormPlugin.PLUGIN_KEY.equals(provider.getParentPluginKey()) && !goverConfig.isFormModuleEnabled();
    }

    private void validateProcessNodeTypeCapacity(@Nonnull ProcessNodeEntity node,
                                                 @Nonnull ProcessNodeDefinition<?> provider,
                                                 @Nullable Integer excludedNodeId) throws ResponseException {
        var type = provider.getType();
        if (goverConfig.isProcessNodeTypeUnlimited(type)) {
            return;
        }

        var limit = goverConfig.getProcessNodeLimit(type);
        var count = countProcessNodesOfType(
                node.getProcessId(),
                node.getProcessVersion(),
                type,
                excludedNodeId
        );

        if (count >= limit) {
            throw ResponseException.badRequest(createProcessNodeLimitExceededMessage(type, limit));
        }
    }

    private void validateProcessNodeTypeBatchCapacity(@Nonnull ProcessNodeType type,
                                                      int count) throws ResponseException {
        if (goverConfig.isProcessNodeTypeUnlimited(type)) {
            return;
        }

        var limit = goverConfig.getProcessNodeLimit(type);
        if (count > limit) {
            throw ResponseException.badRequest(createProcessNodeLimitExceededMessage(type, limit));
        }
    }

    private long countProcessNodesOfType(@Nonnull Integer processId,
                                         @Nonnull Integer processVersion,
                                         @Nonnull ProcessNodeType type,
                                         @Nullable Integer excludedNodeId) throws ResponseException {
        var nodes = processNodeRepository.findAllByProcessIdAndProcessVersion(processId, processVersion);
        long count = 0;

        for (var node : nodes) {
            if (Objects.equals(node.getId(), excludedNodeId)) {
                continue;
            }

            var provider = processNodeProviderService
                    .getProcessNodeDefinition(node)
                    .orElseThrow(() -> ResponseException.internalServerError("No provider found for node with id " + node.getId()));

            if (provider.getType() == type) {
                count++;
            }
        }

        return count;
    }

    private static String createDisabledFormModuleMessage() {
        return "Die Formularerweiterung ist auf dieser Instanz nicht aktiviert. Prozesselemente dieses Plugins können nicht verwendet werden.";
    }

    private static String createProcessNodeLimitExceededMessage(@Nonnull ProcessNodeType type,
                                                                int limit) {
        return "In dieser Prozessversion sind maximal %d Prozesselemente vom Typ %s erlaubt."
                .formatted(limit, StringUtils.quote(getProcessNodeTypeLabel(type)));
    }

    private static String getProcessNodeTypeLabel(@Nonnull ProcessNodeType type) {
        return switch (type) {
            case Trigger -> "Auslöser";
            case Action -> "Aktion";
            case FlowControl -> "Flusselement";
            case Termination -> "Abschluss";
        };
    }

    private static void addCommonError(@Nonnull Map<String, List<String>> commonErrors,
                                       @Nonnull List<String> problems,
                                       @Nonnull String commonErrorKey,
                                       @Nonnull String problemLabel,
                                       @Nonnull String errorMessage) {
        commonErrors
                .computeIfAbsent(commonErrorKey, ignored -> new LinkedList<>())
                .add(errorMessage);
        problems.add(problemLabel + ": " + errorMessage);
    }

    public record ProcessConfigurationDetails<NodeConfig>(
            @Nonnull NodeConfig configuration,
            @Nonnull DerivedRuntimeElementData derivedRuntimeElementData
    ) {
    }
}
