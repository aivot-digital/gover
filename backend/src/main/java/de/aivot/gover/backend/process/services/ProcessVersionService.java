package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.lib.services.EntityService;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntityId;
import de.aivot.gover.backend.process.models.ProcessNodeDefinition;
import de.aivot.gover.backend.process.models.ProcessNodeProblems;
import de.aivot.gover.backend.process.repositories.ProcessVersionRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Service
public class ProcessVersionService implements EntityService<ProcessVersionEntity, ProcessVersionEntityId> {

    private final ProcessVersionRepository processDefinitionVersionRepository;
    private final ProcessNodeService processNodeService;
    private final ProcessNodeDefinitionService processNodeDefinitionService;
    private final CaseNumberGeneratorService caseNumberGeneratorService;

    @Autowired
    public ProcessVersionService(ProcessVersionRepository processDefinitionVersionRepository,
                                 ProcessNodeService processNodeService,
                                 ProcessNodeDefinitionService processNodeDefinitionService,
                                 CaseNumberGeneratorService caseNumberGeneratorService) {
        this.processDefinitionVersionRepository = processDefinitionVersionRepository;
        this.processNodeService = processNodeService;
        this.processNodeDefinitionService = processNodeDefinitionService;
        this.caseNumberGeneratorService = caseNumberGeneratorService;
    }

    @Nonnull
    @Override
    public ProcessVersionEntity create(@Nonnull ProcessVersionEntity entity) throws ResponseException {
        caseNumberGeneratorService.validateCaseNumberTemplate(entity.getCaseNumberTemplate());

        // Fetch the latest version number for the given process definition
        Integer latestVersionNumber = processDefinitionVersionRepository
                .maxVersionForProcessDefinition(entity.getProcessId())
                .orElse(0);

        // Set the new version number to be one greater than the latest version number
        entity.setProcessVersion(latestVersionNumber + 1);

        return processDefinitionVersionRepository
                .save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessVersionEntity> performList(@Nonnull Pageable pageable,
                                                  @Nullable Specification<ProcessVersionEntity> specification,
                                                  @Nullable Filter<ProcessVersionEntity> filter) throws ResponseException {
        return processDefinitionVersionRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessVersionEntity> retrieve(@Nonnull ProcessVersionEntityId id) throws ResponseException {
        return processDefinitionVersionRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessVersionEntity> retrieve(@Nonnull Specification<ProcessVersionEntity> specification) throws ResponseException {
        return processDefinitionVersionRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull ProcessVersionEntityId id) {
        return processDefinitionVersionRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessVersionEntity> specification) {
        return processDefinitionVersionRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessVersionEntity performUpdate(@Nonnull ProcessVersionEntityId id,
                                              @Nonnull ProcessVersionEntity entity,
                                              @Nonnull ProcessVersionEntity existingEntity) throws ResponseException {
        caseNumberGeneratorService.validateCaseNumberTemplate(entity.getCaseNumberTemplate());
        existingEntity.setStatus(entity.getStatus());
        existingEntity.setPublicTitle(entity.getPublicTitle());
        existingEntity.setCaseNumberTemplate(entity.getCaseNumberTemplate());
        existingEntity.setNotes(entity.getNotes());
        return processDefinitionVersionRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessVersionEntity entity) throws ResponseException {
        processDefinitionVersionRepository.delete(entity);
    }

    public List<ProcessNodeProblems> validate(@Nonnull ProcessVersionEntity entity) throws ResponseException {
        var nodes = processNodeService
                .findAllByProcessIdAndProcessVersion(entity.getProcessId(), entity.getProcessVersion());

        var res = new LinkedList<ProcessNodeProblems>();

        for (var node : nodes) {
            var provider = processNodeDefinitionService
                    .getProcessNodeDefinition(node)
                    .orElseThrow(() -> ResponseException.internalServerError("No provider found for node with id " + node.getId()));

            val(node, provider)
                    .ifPresent(res::add);
        }

        return res;
    }

    private <NodeConfig> Optional<ProcessNodeProblems> val(ProcessNodeEntity node, ProcessNodeDefinition<NodeConfig> provider) throws ResponseException {
        return processNodeService
                .validate(node, provider, true);
    }

    public Optional<ProcessVersionEntity> getLatestVersion(Integer processDefinitionId) {
        var maxVersion = processDefinitionVersionRepository
                .maxVersionForProcessDefinition(processDefinitionId)
                .orElse(0);

        if (maxVersion == 0) {
            return Optional.empty();
        }

        var id = new ProcessVersionEntityId(processDefinitionId, maxVersion);
        return processDefinitionVersionRepository.findById(id);
    }
}
