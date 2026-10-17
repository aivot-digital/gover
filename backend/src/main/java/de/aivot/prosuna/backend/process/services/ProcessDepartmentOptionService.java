package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.process.dtos.ProcessDepartmentOptionDTO;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProcessDepartmentOptionService {
    private final ProcessService processService;
    private final EntityManager entityManager;

    public ProcessDepartmentOptionService(ProcessService processService, EntityManager entityManager) {
        this.processService = processService;
        this.entityManager = entityManager;
    }

    @Nonnull
    @Transactional(readOnly = true)
    public List<ProcessDepartmentOptionDTO> listForUser(@Nonnull String userId) {
        var access = processService.getReadAccessSpecification(userId);
        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(ProcessDepartmentOptionDTO.class);
        var process = query.from(ProcessEntity.class);
        var department = query.from(DepartmentEntity.class);
        var predicate = builder.equal(process.get("departmentId"), department.get("id"));
        if (access != null) {
            predicate = builder.and(predicate, access.toPredicate(process, query, builder));
        }

        // A visible process permits naming its owner, not reading the department's full configuration.
        // Do not apply the current search, tab or page: filter options must remain stable while filtering.
        query.select(builder.construct(ProcessDepartmentOptionDTO.class, department.get("id"), department.get("name")))
                .distinct(true)
                .where(predicate)
                .orderBy(builder.asc(department.get("name")), builder.asc(department.get("id")));
        return entityManager.createQuery(query).getResultList();
    }
}
