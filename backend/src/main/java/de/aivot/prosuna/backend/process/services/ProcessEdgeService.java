package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.process.entities.ProcessEdgeEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessEdgeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ProcessEdgeService implements EntityService<ProcessEdgeEntity, Integer> {

    private final ProcessEdgeRepository processDefinitionEdgeRepository;
    private final ProcessNodeRepository processNodeRepository;
    private final ProcessVersionDraftGuard processVersionDraftGuard;

    @Autowired
    public ProcessEdgeService(ProcessEdgeRepository processDefinitionEdgeRepository,
                              ProcessNodeRepository processNodeRepository,
                              ProcessVersionDraftGuard processVersionDraftGuard) {
        this.processDefinitionEdgeRepository = processDefinitionEdgeRepository;
        this.processNodeRepository = processNodeRepository;
        this.processVersionDraftGuard = processVersionDraftGuard;
    }

    @Nonnull
    @Override
    public ProcessEdgeEntity create(@Nonnull ProcessEdgeEntity entity) throws ResponseException {
        requireEditableEdge(entity);
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
        processVersionDraftGuard.requireDrafted(existingEntity.getProcessId(), existingEntity.getProcessVersion());
        requireEditableEdge(entity);
        existingEntity.setProcessId(entity.getProcessId());
        existingEntity.setProcessVersion(entity.getProcessVersion());
        existingEntity.setFromNodeId(entity.getFromNodeId());
        existingEntity.setToNodeId(entity.getToNodeId());
        existingEntity.setViaPort(entity.getViaPort());
        return processDefinitionEdgeRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessEdgeEntity entity) throws ResponseException {
        processVersionDraftGuard.requireDrafted(entity.getProcessId(), entity.getProcessVersion());
        processDefinitionEdgeRepository.delete(entity);
    }

    private void requireEditableEdge(@Nonnull ProcessEdgeEntity edge) throws ResponseException {
        processVersionDraftGuard.requireDrafted(edge.getProcessId(), edge.getProcessVersion());

        // The workflow engine follows an edge by its start node and port, regardless of the edge's process version.
        // An edge must therefore connect nodes of its own version, otherwise a draft edge could change the flow of a
        // published version.
        requireNodeOfEdgeVersion(edge.getFromNodeId(), edge);
        requireNodeOfEdgeVersion(edge.getToNodeId(), edge);
    }

    private void requireNodeOfEdgeVersion(@Nullable Integer nodeId,
                                          @Nonnull ProcessEdgeEntity edge) throws ResponseException {
        var belongsToEdgeVersion = nodeId != null && processNodeRepository
                .findById(nodeId)
                .filter(node -> Objects.equals(node.getProcessId(), edge.getProcessId())
                        && Objects.equals(node.getProcessVersion(), edge.getProcessVersion()))
                .isPresent();

        if (!belongsToEdgeVersion) {
            throw ResponseException.badRequest("Eine Prozesskante kann nur Prozesselemente derselben Prozessversion verbinden.");
        }
    }

    public List<ProcessEdgeEntity> findAllByProcessIdAndProcessVersion(Integer id, Integer processVersion) {
        return processDefinitionEdgeRepository
                .findAllByProcessIdAndProcessVersion(id, processVersion);
    }
}

