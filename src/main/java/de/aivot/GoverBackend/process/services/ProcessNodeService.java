package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntityId;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinitionContextConfig;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ProcessNodeService implements EntityService<ProcessNodeEntity, Integer> {

    private final ProcessNodeRepository processDefinitionNodeRepository;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ElementDerivationService elementDerivationService;
    private final UserService userService;
    private final ProcessRepository processDefinitionRepository;
    private final ProcessVersionRepository processDefinitionVersionRepository;

    @Autowired
    public ProcessNodeService(ProcessNodeRepository processDefinitionNodeRepository,
                              ProcessNodeDefinitionService processNodeProviderService,
                              ElementDerivationService elementDerivationService,
                              UserService userService,
                              ProcessRepository processDefinitionRepository,
                              ProcessVersionRepository processDefinitionVersionRepository) {
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.processNodeProviderService = processNodeProviderService;
        this.elementDerivationService = elementDerivationService;
        this.userService = userService;
        this.processDefinitionRepository = processDefinitionRepository;
        this.processDefinitionVersionRepository = processDefinitionVersionRepository;
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
        processNodeProviderService
                .getProcessNodeDefinition(entity.getProcessNodeDefinitionKey(), entity.getProcessNodeDefinitionVersion())
                .orElseThrow(() -> ResponseException.badRequest(
                        "Der Prozesselement-Funktionsanbieter %s (Version %s) existiert nicht.",
                        StringUtils.quote(entity.getProcessNodeDefinitionKey()),
                        entity.getProcessNodeDefinitionVersion()
                ));

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
        var providerChanged =
                !existingEntity.getProcessNodeDefinitionKey().equals(entity.getProcessNodeDefinitionKey()) ||
                existingEntity.getProcessNodeDefinitionVersion() != entity.getProcessNodeDefinitionVersion();

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

        // A provider replacement intentionally starts from a fresh configuration. In that case we
        // must derive the new provider defaults with skipped validation errors first, just like the
        // create flow does, otherwise the update endpoint would reject the empty reset state before
        // the user even has a chance to configure the new node.
        var derivedObjectItemData = deriveConfiguration(entity, providerChanged, null);

        // If derivation has errors, throw bad request
        if (derivedObjectItemData.hasAnyError()) {
            throw ResponseException.badRequest(derivedObjectItemData);
        }

        existingEntity.setConfiguration(entity.getConfiguration());

        if (providerChanged) {
            return processDefinitionNodeRepository.save(existingEntity);
        }

        // Fetch the provider and validate configuration
        var provider = processNodeProviderService
                .getProcessNodeDefinition(entity.getProcessNodeDefinitionKey(), entity.getProcessNodeDefinitionVersion())
                .orElseThrow(ResponseException::badRequest);
        provider.validateConfiguration(entity, entity.getConfiguration(), derivedObjectItemData);

        return processDefinitionNodeRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessNodeEntity entity) throws ResponseException {
        processDefinitionNodeRepository.delete(entity);
    }

    @Nonnull
    public DerivedRuntimeElementData deriveConfiguration(@Nonnull ProcessNodeEntity entity,
                                                         boolean skipErrors) throws ResponseException {
        return deriveConfiguration(entity, skipErrors, null);
    }

    @Nonnull
    public DerivedRuntimeElementData deriveConfiguration(@Nonnull ProcessNodeEntity entity,
                                                         boolean skipErrors,
                                                         @Nullable UserEntity user) throws ResponseException {
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

        var provider = processNodeProviderService
                .getProcessNodeDefinition(entity.getProcessNodeDefinitionKey(), entity.getProcessNodeDefinitionVersion())
                .orElseThrow(ResponseException::badRequest);

        var context = new ProcessNodeDefinitionContextConfig(
                user,
                processDefinition,
                processVersion,
                entity
        );

        var layout = provider
                .getConfigurationLayout(context);

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

        return derivedData;
    }

    public Set<String> getAllUsedDataKeys(@Nonnull Integer processId, @Nonnull Integer processVersion) {
        return processDefinitionNodeRepository.findAllDataKeysByProcessIdAndVersion(processId, processVersion);
    }
}
