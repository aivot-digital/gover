package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.permissions.entities.VUserDepartmentPermissionEntity;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.repositories.ProcessRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessService implements EntityService<ProcessEntity, Integer> {

    private final ProcessRepository processDefinitionRepository;

    @Autowired
    public ProcessService(ProcessRepository processDefinitionRepository) {
        this.processDefinitionRepository = processDefinitionRepository;
    }

    @Nonnull
    @Override
    public ProcessEntity create(@Nonnull ProcessEntity entity) throws ResponseException {
        entity.setId(null);
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
        Specification<ProcessEntity> userAccessSpec = (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("departmentId")),
                        root.get("departmentId").in(
                                /*query
                                        .subquery(Integer.class)
                                        .select(criteriaBuilder.("departmentId"))
                                        .from(VUserDepartmentPermissionEntity.class)
                                        .where(criteriaBuilder.equal(root.get("id"), userId))

                                 */
                        )
                );

        Specification<ProcessEntity> combinedSpec = (specification == null) ? userAccessSpec : specification.and(userAccessSpec);

        return processDefinitionRepository.findAll(specification, pageable);
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
