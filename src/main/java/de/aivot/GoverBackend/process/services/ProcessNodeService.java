package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.elements.utils.ElementStreamUtils;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessEdgeEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntityId;
import de.aivot.GoverBackend.process.filters.ProcessNodeFilter;
import de.aivot.GoverBackend.process.models.ProcessDataKeyHint;
import de.aivot.GoverBackend.process.models.ProcessDataKeyHintResponse;
import de.aivot.GoverBackend.process.models.ProcessDataKeyHintType;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.ProcessNodeProblems;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.GoverBackend.process.repositories.ProcessEdgeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessRepository;
import de.aivot.GoverBackend.process.repositories.ProcessVersionRepository;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
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

    private final ProcessNodeRepository processDefinitionNodeRepository;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ElementDerivationService elementDerivationService;
    private final UserService userService;
    private final ProcessRepository processDefinitionRepository;
    private final ProcessVersionRepository processDefinitionVersionRepository;
    private final ProcessEdgeRepository processEdgeRepository;

    @Autowired
    public ProcessNodeService(ProcessNodeRepository processDefinitionNodeRepository,
                              ProcessNodeDefinitionService processNodeProviderService,
                              ElementDerivationService elementDerivationService,
                              UserService userService,
                              ProcessRepository processDefinitionRepository,
                              ProcessVersionRepository processDefinitionVersionRepository, ProcessEdgeRepository processEdgeRepository) {
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.processNodeProviderService = processNodeProviderService;
        this.elementDerivationService = elementDerivationService;
        this.userService = userService;
        this.processDefinitionRepository = processDefinitionRepository;
        this.processDefinitionVersionRepository = processDefinitionVersionRepository;
        this.processEdgeRepository = processEdgeRepository;
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

        if (entity.getName() == null || StringUtils.isNullOrEmpty(entity.getName())) {
            entity.setName(provider.getName());
        }

        // Save the process node.
        return processDefinitionNodeRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessNodeEntity> performList(@Nonnull Pageable pageable,
                                               @Nullable Specification<ProcessNodeEntity> specification,
                                               @Nullable Filter<ProcessNodeEntity> filter) throws ResponseException {
        return processDefinitionNodeRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return processDefinitionNodeRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeEntity> retrieve(@Nonnull Specification<ProcessNodeEntity> specification) throws ResponseException {
        return processDefinitionNodeRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return processDefinitionNodeRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessNodeEntity> specification) {
        return processDefinitionNodeRepository.exists(specification);
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

        return processDefinitionNodeRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessNodeEntity entity) throws ResponseException {
        processDefinitionNodeRepository.delete(entity);
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
        var dummyLogger = new ElementDerivationLogger();
        var derivedData = elementDerivationService.derive(edr, dummyLogger);

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
        return processDefinitionNodeRepository.findAllDataKeysByProcessIdAndVersion(processId, processVersion);
    }

    public List<ProcessNodeEntity> findAllByProcessIdAndProcessVersion(Integer processId, Integer processVersion) {
        return processDefinitionNodeRepository
                .findAllByProcessIdAndProcessVersion(processId, processVersion);
    }

    @Nonnull
    public List<ProcessDataKeyHintResponse> getProcessDataKeyHintResponses(@Nonnull ProcessNodeEntity node) throws ResponseException {
        var processNodesById = new LinkedHashMap<Integer, ProcessNodeEntity>();

        processDefinitionNodeRepository
                .findAllByProcessIdAndProcessVersion(node.getProcessId(), node.getProcessVersion())
                .stream()
                .sorted(Comparator.comparing(ProcessNodeEntity::getId))
                .forEach(processNode -> processNodesById.put(processNode.getId(), processNode));

        var incomingEdgesByNodeId = buildIncomingEdgesByNodeId(
                processEdgeRepository.findAllByProcessIdAndProcessVersion(node.getProcessId(), node.getProcessVersion())
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

        var currentHints = new ArrayList<ProcessDataKeyHint>();
        List<ProcessDataKeyHintResponse> responses = new ArrayList<>();

        for (var previousNode : previousNodes) {
            var hintCalculationResult = calculateProcessDataKeyHintsForNode(previousNode, currentHints);
            responses = mergeProcessDataKeyHintResponses(
                    responses,
                    hintCalculationResult.hints(),
                    hintCalculationResult.contributedKeys(),
                    previousNode
            );
            currentHints = new ArrayList<>(hintCalculationResult.hints());
        }

        return responses;
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
    @SuppressWarnings("unchecked")
    private <NodeConfig> ProcessDataKeyHintCalculationResult calculateProcessDataKeyHintsForNode(@Nonnull ProcessNodeEntity node,
                                                                                                  @Nonnull List<ProcessDataKeyHint> previousDataKeyHints) throws ResponseException {
        var provider = (ProcessNodeDefinition<NodeConfig>) processNodeProviderService
                .getProcessNodeDefinition(node)
                .orElseThrow(ResponseException::badRequest);

        var configuration = deriveConfiguration(node, provider, null, true);
        var providerHints = provider.calculateProcessDataKeyHints(
                node,
                configuration.configuration(),
                previousDataKeyHints
        );

        var updatedHints = providerHints != null ? providerHints : previousDataKeyHints;
        var contributedKeys = getContributedHintKeys(previousDataKeyHints, updatedHints);
        var outputMappingHints = getOutputMappingProcessDataKeyHints(node, provider);

        for (var outputMappingHint : outputMappingHints) {
            contributedKeys.add(outputMappingHint.key());
        }

        var mergedHints = new ArrayList<>(updatedHints);
        mergedHints.addAll(outputMappingHints);

        return new ProcessDataKeyHintCalculationResult(
                deduplicateProcessDataKeyHintsByKey(mergedHints),
                contributedKeys
        );
    }

    @Nonnull
    private List<ProcessDataKeyHintResponse> mergeProcessDataKeyHintResponses(@Nonnull List<ProcessDataKeyHintResponse> existingResponses,
                                                                              @Nonnull List<ProcessDataKeyHint> updatedHints,
                                                                              @Nonnull Set<String> contributedKeys,
                                                                              @Nonnull ProcessNodeEntity currentNode) {
        var sourceNodeByKey = new LinkedHashMap<String, ProcessNodeEntity>();
        for (var existingResponse : existingResponses) {
            sourceNodeByKey.put(existingResponse.key(), existingResponse.node());
        }

        var mergedResponses = new ArrayList<ProcessDataKeyHintResponse>();
        for (var updatedHint : updatedHints) {
            mergedResponses.add(new ProcessDataKeyHintResponse(
                    updatedHint.key(),
                    updatedHint.type(),
                    contributedKeys.contains(updatedHint.key())
                            ? currentNode
                            : sourceNodeByKey.getOrDefault(updatedHint.key(), currentNode)
            ));
        }

        return mergedResponses;
    }

    @Nonnull
    private Set<String> getContributedHintKeys(@Nonnull List<ProcessDataKeyHint> previousHints,
                                               @Nonnull List<ProcessDataKeyHint> updatedHints) {
        var previousHintsByKey = new LinkedHashMap<String, ProcessDataKeyHint>();
        for (var previousHint : previousHints) {
            previousHintsByKey.put(previousHint.key(), previousHint);
        }

        var contributedKeys = new LinkedHashSet<String>();
        var seenKeys = new HashSet<String>();

        for (var updatedHint : updatedHints) {
            var previousHint = previousHintsByKey.get(updatedHint.key());
            var hasSameKeyBeenSeenBefore = !seenKeys.add(updatedHint.key());
            var isNewOrChangedHint = previousHint == null || !Objects.equals(previousHint, updatedHint);

            if (hasSameKeyBeenSeenBefore || isNewOrChangedHint) {
                contributedKeys.remove(updatedHint.key());
                contributedKeys.add(updatedHint.key());
            }
        }

        return contributedKeys;
    }

    @Nonnull
    private <NodeConfig> List<ProcessDataKeyHint> getOutputMappingProcessDataKeyHints(@Nonnull ProcessNodeEntity node,
                                                                                       @Nonnull ProcessNodeDefinition<NodeConfig> provider) {
        var hints = new ArrayList<ProcessDataKeyHint>();

        for (var output : provider.getOutputs()) {
            var mappedProcessDataKey = StringUtils.toNullableTrimmedString(node.getOutputMappings().get(output.key()));
            if (mappedProcessDataKey == null) {
                continue;
            }

            hints.add(new ProcessDataKeyHint(
                    mappedProcessDataKey,
                    ProcessDataKeyHintType.ProcessData
            ));
        }

        return hints;
    }

    @Nonnull
    private List<ProcessDataKeyHint> deduplicateProcessDataKeyHintsByKey(@Nonnull List<ProcessDataKeyHint> hints) {
        var hintsByKey = new LinkedHashMap<String, ProcessDataKeyHint>();

        for (var hint : hints) {
            hintsByKey.remove(hint.key());
            hintsByKey.put(hint.key(), hint);
        }

        return new ArrayList<>(hintsByKey.values());
    }

    @Nonnull
    public <NodeConfig> Optional<ProcessNodeProblems> validate(@Nonnull ProcessNodeEntity node,
                                                               @Nonnull ProcessNodeDefinition<NodeConfig> provider,
                                                               @Nonnull Boolean checkPorts) throws ResponseException {
        var commonErrors = new HashMap<String, String>();
        var problems = new LinkedList<String>();

        if (StringUtils.isNullOrEmpty(node.getDataKey())) {
            var commonErrorMessage = "Der Datenschlüssel darf nicht leer sein.";
            commonErrors.put(ProcessNodeProblems.COMMON_ERROR_KEY_DATA_KEY, commonErrorMessage);
            problems.add("Datenschlüssel: " + commonErrorMessage);
        } else {
            var duplicateDataKeyFilter = ProcessNodeFilter
                    .create()
                    .setNotId(node.getId())
                    .setDataKey(node.getDataKey())
                    .setProcessId(node.getProcessId())
                    .setProcessVersion(node.getProcessVersion());

            if (this.exists(duplicateDataKeyFilter)) {
                var commonErrorMessage = "Es existiert mindestens ein weiterer Knoten mit dem selben Datenschlüssel. Datenschlüssel müssen eindeutig sein.";
                commonErrors.put(ProcessNodeProblems.COMMON_ERROR_KEY_DATA_KEY, commonErrorMessage);
                problems.add("Datenschlüssel: " + commonErrorMessage);
            }
        }

        var layout = getConfigLayoutElement(node, provider, null);

        ProcessConfigurationDetails<NodeConfig> derivedConfiguration;
        try {
            derivedConfiguration = this
                    .deriveConfiguration(node, provider,  null,false);
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
                    layout.findChild(err.getKey(), BaseInputElement.class).ifPresentOrElse(
                            element -> problems.add(element.getLabel() + ": " + err.getValue()),
                            () -> problems.add("Element mit ID " + err.getKey() + ": " + err.getValue())
                    );
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

            return Optional.of(new ProcessNodeProblems(node, problems, commonErrors, derivedConfiguration != null ? derivedConfiguration.derivedRuntimeElementData : new DerivedRuntimeElementData()));
        }
    }

    public record ProcessConfigurationDetails<NodeConfig>(
            @Nonnull NodeConfig configuration,
            @Nonnull DerivedRuntimeElementData derivedRuntimeElementData
    ) {
    }

    private record ProcessDataKeyHintCalculationResult(
            @Nonnull List<ProcessDataKeyHint> hints,
            @Nonnull Set<String> contributedKeys
    ) {
    }
}
