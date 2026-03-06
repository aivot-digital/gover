package de.aivot.GoverBackend.audit.services;

import de.aivot.GoverBackend.audit.entities.AuditLogEntity;
import de.aivot.GoverBackend.audit.repositories.AuditLogRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.CreateEntityService;
import de.aivot.GoverBackend.lib.services.ListEntityService;
import de.aivot.GoverBackend.lib.services.RetrieveEntityService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuditLogService implements CreateEntityService<AuditLogEntity>, ListEntityService<AuditLogEntity>, RetrieveEntityService<AuditLogEntity, Long> {
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Nonnull
    @Override
    public AuditLogEntity create(@Nonnull AuditLogEntity entity) throws ResponseException {
        var now = LocalDateTime.now();

        entity.setId(null);

        if (entity.getEventTs() == null) {
            entity.setEventTs(now);
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        if (entity.getTags() == null) {
            entity.setTags(List.of());
        }
        if (entity.getMetadata() == null) {
            entity.setMetadata(Map.of());
        }
        if (entity.getActionResult() == null) {
            entity.setActionResult("success");
        }
        if (entity.getActorType() == null) {
            entity.setActorType("SYSTEM");
        }

        return auditLogRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<AuditLogEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<AuditLogEntity> specification,
            @Nullable Filter<AuditLogEntity> filter
    ) throws ResponseException {
        if (specification == null) {
            return auditLogRepository.findAll(pageable);
        }

        return auditLogRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<AuditLogEntity> retrieve(@Nonnull Long id) throws ResponseException {
        return auditLogRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<AuditLogEntity> retrieve(@Nonnull Specification<AuditLogEntity> specification) throws ResponseException {
        return auditLogRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Long id) {
        return auditLogRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<AuditLogEntity> specification) {
        return auditLogRepository.exists(specification);
    }
}
