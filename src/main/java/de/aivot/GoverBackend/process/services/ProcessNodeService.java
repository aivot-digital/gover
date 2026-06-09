package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
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
import de.aivot.GoverBackend.process.models.*;
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

    private final ProcessNodeRepository processNodeRepository;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ElementDerivationService elementDerivationService;
    private final UserService userService;
    private final ProcessRepository processDefinitionRepository;
    private final ProcessVersionRepository processDefinitionVersionRepository;
    private final ProcessEdgeRepository processEdgeRepository;

    @Autowired
    public ProcessNodeService(ProcessNodeRepository processNodeRepository,
                              ProcessNodeDefinitionService processNodeProviderService,
                              ElementDerivationService elementDerivationService,
                              UserService userService,
                              ProcessRepository processDefinitionRepository,
                              ProcessVersionRepository processDefinitionVersionRepository, ProcessEdgeRepository processEdgeRepository) {
        this.processNodeRepository = processNodeRepository;
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

            return Optional.of(new ProcessNodeProblems(node, problems, commonErrors,
                    derivedConfiguration != null ? derivedConfiguration.derivedRuntimeElementData : new DerivedRuntimeElementData()));
        }
    }

    public record ProcessConfigurationDetails<NodeConfig>(
            @Nonnull NodeConfig configuration,
            @Nonnull DerivedRuntimeElementData derivedRuntimeElementData
    ) {
    }
}
