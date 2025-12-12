package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionVersionEntityId;
import de.aivot.GoverBackend.process.repositories.ProcessDefinitionNodeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessDefinitionRepository;
import de.aivot.GoverBackend.process.repositories.ProcessDefinitionVersionRepository;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
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
import java.util.Map;
import java.util.Optional;

@Service
public class ProcessDefinitionNodeService implements EntityService<ProcessDefinitionNodeEntity, Integer> {

    private final ProcessDefinitionNodeRepository processDefinitionNodeRepository;
    private final ProcessNodeProviderService processNodeProviderService;
    private final ElementDerivationService elementDerivationService;
    private final UserService userService;
    private final ProcessDefinitionRepository processDefinitionRepository;
    private final ProcessDefinitionVersionRepository processDefinitionVersionRepository;

    @Autowired
    public ProcessDefinitionNodeService(ProcessDefinitionNodeRepository processDefinitionNodeRepository, ProcessNodeProviderService processNodeProviderService, ElementDerivationService elementDerivationService, UserService userService, ProcessDefinitionRepository processDefinitionRepository, ProcessDefinitionVersionRepository processDefinitionVersionRepository) {
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
        this.processNodeProviderService = processNodeProviderService;
        this.elementDerivationService = elementDerivationService;
        this.userService = userService;
        this.processDefinitionRepository = processDefinitionRepository;
        this.processDefinitionVersionRepository = processDefinitionVersionRepository;
    }

    @Nonnull
    @Override
    public ProcessDefinitionNodeEntity create(@Nonnull ProcessDefinitionNodeEntity entity) throws ResponseException {
        entity.setId(null);

        var derivedObjectItemData = deriveDataObjectItemData(entity, true);
        entity.setConfiguration(derivedObjectItemData);

        /*
        var provider = processNodeProviderService
                .getProcessNodeProvider(entity.getCodeKey())
                .orElseThrow(ResponseException::badRequest);

        provider.validateConfiguration(entity);
         */

        return processDefinitionNodeRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessDefinitionNodeEntity> performList(@Nonnull Pageable pageable,
                                                         @Nullable Specification<ProcessDefinitionNodeEntity> specification,
                                                         @Nullable Filter<ProcessDefinitionNodeEntity> filter) throws ResponseException {
        return processDefinitionNodeRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessDefinitionNodeEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return processDefinitionNodeRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessDefinitionNodeEntity> retrieve(@Nonnull Specification<ProcessDefinitionNodeEntity> specification) throws ResponseException {
        return processDefinitionNodeRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return processDefinitionNodeRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessDefinitionNodeEntity> specification) {
        return processDefinitionNodeRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessDefinitionNodeEntity performUpdate(@Nonnull Integer id,
                                                     @Nonnull ProcessDefinitionNodeEntity entity,
                                                     @Nonnull ProcessDefinitionNodeEntity existingEntity) throws ResponseException {
        existingEntity.setProcessDefinitionId(entity.getProcessDefinitionId());
        existingEntity.setProcessDefinitionVersion(entity.getProcessDefinitionVersion());
        existingEntity.setDataKey(entity.getDataKey());
        existingEntity.setCodeKey(entity.getCodeKey());

        // Derive configuration
        var derivedObjectItemData = deriveDataObjectItemData(entity, false);
        existingEntity.setConfiguration(derivedObjectItemData);

        var provider = processNodeProviderService
                .getProcessNodeProvider(entity.getCodeKey())
                .orElseThrow(ResponseException::badRequest);

        provider.validateConfiguration(entity);

        return processDefinitionNodeRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessDefinitionNodeEntity entity) throws ResponseException {
        processDefinitionNodeRepository.delete(entity);
    }

    @Nonnull
    private ElementData deriveDataObjectItemData(@Nonnull ProcessDefinitionNodeEntity entity, boolean skipErrors) throws ResponseException {
        UserEntity user = null;
        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof Jwt jwt) {
            user = userService
                    .fromJWT(jwt)
                    .orElseThrow(ResponseException::unauthorized);
        }
        if (user == null) {
            throw ResponseException.unauthorized();
        }

        var processDefinition = processDefinitionRepository
                .findById(entity.getProcessDefinitionId())
                .orElseThrow(ResponseException::badRequest);

        var processVersion = processDefinitionVersionRepository
                .findById(ProcessDefinitionVersionEntityId.of(processDefinition.getId(), entity.getProcessDefinitionVersion()))
                .orElseThrow(ResponseException::badRequest);

        var provider = processNodeProviderService
                .getProcessNodeProvider(entity.getCodeKey())
                .orElseThrow(ResponseException::badRequest);

        var layout = provider
                .getConfigurationLayout(
                        user,
                        processDefinition,
                        processVersion,
                        entity
                );

        var edo = new ElementDerivationOptions();

        if (skipErrors) {
            edo.setSkipErrorsForElementIds(List.of(ElementDerivationOptions.ALL_ELEMENTS));
        }

        var edr = new ElementDerivationRequest()
                .setElement(layout)
                .setElementData(entity.getConfiguration())
                .setOptions(edo);
        var dummyLogger = new ElementDerivationLogger();
        var derivedData = elementDerivationService.derive(edr, dummyLogger);

        if (derivedData.hasAnyError()) {
            throw ResponseException
                    .badRequest(derivedData);
        }

        return derivedData;
    }
}

