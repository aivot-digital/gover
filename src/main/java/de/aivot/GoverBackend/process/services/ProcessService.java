package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.permissions.ProcessPermissionProvider;
import de.aivot.GoverBackend.process.repositories.ProcessRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessService implements EntityService<ProcessEntity, Integer> {

    private final ProcessRepository processDefinitionRepository;
    private final PermissionService permissionService;

    @Autowired
    public ProcessService(ProcessRepository processDefinitionRepository,
                          PermissionService permissionService) {
        this.processDefinitionRepository = processDefinitionRepository;
        this.permissionService = permissionService;
    }

    @Nonnull
    @Override
    public ProcessEntity create(@Nonnull ProcessEntity entity) throws ResponseException {
        entity.setId(null);
        entity.setAccessKey(UUID.randomUUID());
        return processDefinitionRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessEntity> performList(@Nonnull Pageable pageable,
                                           @Nullable Specification<ProcessEntity> specification,
                                           @Nullable Filter<ProcessEntity> filter) throws ResponseException {
        return processDefinitionRepository.findAll(specification, pageable);
    }

    public Page<ProcessEntity> listAllByAccessibleForUser(@Nonnull Pageable pageable,
                                                          @Nonnull String userId,
                                                          @Nullable Specification<ProcessEntity> specification) throws ResponseException {
        if (permissionService.hasSystemPermission(userId, ProcessPermissionProvider.PROCESS_DEFINITION_READ)) {
            return processDefinitionRepository.findAll(specification, pageable);
        }

        var accessibleDepartmentIds = permissionService
                .getDepartmentsWithPermission(userId, ProcessPermissionProvider.PROCESS_DEFINITION_READ)
                .stream()
                .filter(Objects::nonNull)
                .toList();

        var accessibleProcessIds = processDefinitionRepository
                .getProcessIdsWithPermission(userId, ProcessPermissionProvider.PROCESS_DEFINITION_READ)
                .stream()
                .filter(Objects::nonNull)
                .toList();

        if (accessibleDepartmentIds.isEmpty() && accessibleProcessIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Specification<ProcessEntity> userAccessSpec = (root, query, criteriaBuilder) -> {
            var predicates = new LinkedList<Predicate>();

            if (!accessibleDepartmentIds.isEmpty()) {
                predicates.add(root.get("departmentId").in(accessibleDepartmentIds));
            }

            if (!accessibleProcessIds.isEmpty()) {
                predicates.add(root.get("id").in(accessibleProcessIds));
            }

            return criteriaBuilder.or(predicates.toArray(Predicate[]::new));
        };

        Specification<ProcessEntity> combinedSpec = (specification == null) ? userAccessSpec : specification.and(userAccessSpec);

        return processDefinitionRepository.findAll(combinedSpec, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return processDefinitionRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessEntity> retrieve(@Nonnull Specification<ProcessEntity> specification) throws ResponseException {
        return processDefinitionRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return processDefinitionRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessEntity> specification) {
        return processDefinitionRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessEntity performUpdate(@Nonnull Integer id,
                                       @Nonnull ProcessEntity entity,
                                       @Nonnull ProcessEntity existingEntity) throws ResponseException {
        existingEntity.setInternalTitle(entity.getInternalTitle());
        existingEntity.setDepartmentId(entity.getDepartmentId());
        return processDefinitionRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessEntity entity) throws ResponseException {
        processDefinitionRepository.delete(entity);
    }
}
