package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.process.entities.ProcessEdgeEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessEdgeRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Objects;

@Service
public class ProcessEdgeService implements EntityService<ProcessEdgeEntity, Integer> {

    private final ProcessEdgeRepository processDefinitionEdgeRepository;

    private final ProcessNodeService processNodeService;
    private final ProcessVersionDraftGuard processVersionDraftGuard;

    @Autowired
    public ProcessEdgeService(ProcessEdgeRepository processDefinitionEdgeRepository,
                              ProcessNodeService processNodeService,
                              ProcessVersionDraftGuard processVersionDraftGuard) {
        this.processDefinitionEdgeRepository = processDefinitionEdgeRepository;
        this.processNodeService = processNodeService;
        this.processVersionDraftGuard = processVersionDraftGuard;
    }

    @Nonnull
    @Override
    public ProcessEdgeEntity create(@Nonnull ProcessEdgeEntity entity) throws ResponseException {
        processVersionDraftGuard.requireStructureEditable(entity.getProcessId(), entity.getProcessVersion());
        entity.setId(null);
        validateEndpoints(entity);
        return processDefinitionEdgeRepository.save(entity);
    }

    /**
     * Copies a stored connection into a new draft, e.g. for a new version or an import. Obsolete connections stay
     * visible for correction and block publication there, instead of aborting the non-transactional copy midway.
     */
    @Nonnull
    public ProcessEdgeEntity copy(@Nonnull ProcessEdgeEntity entity) {
        entity.setId(null);
        return processDefinitionEdgeRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessEdgeEntity> performList(@Nonnull Pageable pageable,
                                               @Nullable Specification<ProcessEdgeEntity> specification,
                                               @Nullable Filter<ProcessEdgeEntity> filter) throws ResponseException {
        return processDefinitionEdgeRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessEdgeEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return processDefinitionEdgeRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessEdgeEntity> retrieve(@Nonnull Specification<ProcessEdgeEntity> specification) throws ResponseException {
        return processDefinitionEdgeRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return processDefinitionEdgeRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessEdgeEntity> specification) {
        return processDefinitionEdgeRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessEdgeEntity performUpdate(@Nonnull Integer id,
                                           @Nonnull ProcessEdgeEntity entity,
                                           @Nonnull ProcessEdgeEntity existingEntity) throws ResponseException {
        processVersionDraftGuard.requireStructureEditable(existingEntity.getProcessId(), existingEntity.getProcessVersion());
        processVersionDraftGuard.requireStructureEditable(entity.getProcessId(), entity.getProcessVersion());
        validateEndpoints(entity);
        existingEntity.setProcessId(entity.getProcessId());
        existingEntity.setProcessVersion(entity.getProcessVersion());
        existingEntity.setFromNodeId(entity.getFromNodeId());
        existingEntity.setToNodeId(entity.getToNodeId());
        existingEntity.setViaPort(entity.getViaPort());
        return processDefinitionEdgeRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessEdgeEntity entity) throws ResponseException {
        processVersionDraftGuard.requireStructureEditable(entity.getProcessId(), entity.getProcessVersion());
        processDefinitionEdgeRepository.delete(entity);
    }

    private void validateEndpoints(@Nonnull ProcessEdgeEntity edge) throws ResponseException {
        var source = processNodeService.retrieve(edge.getFromNodeId()).orElseThrow(ResponseException::badRequest);
        var target = processNodeService.retrieve(edge.getToNodeId()).orElseThrow(ResponseException::badRequest);
        // The workflow engine follows an edge by its start node and port, regardless of the edge's process version.
        // An edge must therefore connect nodes of its own version, otherwise a draft edge could change the flow of a
        // published version.
        for (var node : List.of(source, target)) {
            if (!Objects.equals(node.getProcessId(), edge.getProcessId()) || !Objects.equals(node.getProcessVersion(), edge.getProcessVersion())) {
                throw ResponseException.badRequest("Quelle und Ziel der Verbindung müssen zur angegebenen Prozessversion gehören.");
            }
        }
        var resolution = processNodeService.resolvePorts(source);
        if (!resolution.problems().isEmpty() || resolution.ports().stream().noneMatch(port -> port.key().equals(edge.getViaPort()))) {
            throw ResponseException.badRequest("Der ausgewählte Ausgang ist für dieses Prozesselement nicht verfügbar. Prüfen Sie die Konfiguration.");
        }
        var existing = processDefinitionEdgeRepository.findByFromNodeIdAndViaPort(edge.getFromNodeId(), edge.getViaPort());
        if (existing.isPresent() && !Objects.equals(existing.get().getId(), edge.getId())) {
            throw ResponseException.badRequest("Dieser Ausgang ist bereits verbunden.");
        }
    }

    public List<ProcessEdgeEntity> findAllByProcessIdAndProcessVersion(Integer id, Integer processVersion) {
        return processDefinitionEdgeRepository
                .findAllByProcessIdAndProcessVersion(id, processVersion);
    }
}

