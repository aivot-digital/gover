package de.aivot.gover.backend.department.services;

import de.aivot.gover.backend.department.entities.DepartmentEntity;
import de.aivot.gover.backend.department.repositories.DepartmentRepository;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.lib.services.EntityService;
import de.aivot.gover.backend.system.services.SystemService;
import de.aivot.gover.backend.theme.entities.ThemeEntity;
import de.aivot.gover.backend.theme.repositories.ThemeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class DepartmentService implements EntityService<DepartmentEntity, Integer> {
    private final DepartmentRepository departmentRepository;
    private final ThemeRepository themeRepository;
    private final SystemService systemService;

    @Autowired
    public DepartmentService(DepartmentRepository departmentRepository,
                             ThemeRepository themeRepository, SystemService systemService) {
        this.departmentRepository = departmentRepository;
        this.themeRepository = themeRepository;
        this.systemService = systemService;
    }

    @Nonnull
    @Override
    public DepartmentEntity create(@Nonnull DepartmentEntity entity) throws ResponseException {
        entity.setId(null);
        validateParentHierarchy(null, entity.getParentDepartmentId());

        // Check theme existence and set to null if not exists
        var themeId = entity.getThemeId();
        if (themeId != null) {
            var themeExists = themeRepository
                    .existsById(themeId);

            if (!themeExists) {
                entity.setThemeId(null);
            }
        }

        return departmentRepository
                .save(entity);
    }

    @Nonnull
    @Override
    public Page<DepartmentEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<DepartmentEntity> specification,
            Filter<DepartmentEntity> filter) {
        return departmentRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<DepartmentEntity> retrieve(@Nonnull Integer id) {
        return departmentRepository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<DepartmentEntity> retrieve(
            @Nonnull Specification<DepartmentEntity> specification
    ) {
        return departmentRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return departmentRepository.existsById(id);
    }

    @Override
    public boolean exists(
            @Nonnull Specification<DepartmentEntity> specification
    ) {
        return departmentRepository.exists(specification);
    }

    @Nonnull
    @Override
    public DepartmentEntity performUpdate(
            @Nonnull Integer id,
            @Nonnull DepartmentEntity entity,
            @Nonnull DepartmentEntity existingDepartment
    ) throws ResponseException {
        // Copy static fields
        entity.setId(existingDepartment.getId());
        entity.setCreated(existingDepartment.getCreated());
        entity.setUpdated(Instant.now());
        entity.setDepth(existingDepartment.getDepth());
        validateParentHierarchy(id, entity.getParentDepartmentId());

        // Check theme existence and set to null if not exists
        var themeId = entity.getThemeId();
        if (themeId != null) {
            var themeExists = themeRepository
                    .existsById(themeId);

            if (!themeExists) {
                entity.setThemeId(null);
            }
        }

        entity.setParentDepartmentId(entity.getParentDepartmentId());

        return departmentRepository
                .save(entity);
    }

    private void validateParentHierarchy(
            @Nullable Integer departmentId,
            @Nullable Integer requestedParentDepartmentId
    ) throws ResponseException {
        if (requestedParentDepartmentId == null) {
            return;
        }

        if (departmentId != null && requestedParentDepartmentId.equals(departmentId)) {
            throw ResponseException.badRequest("Eine Organisationseinheit kann nicht sich selbst als übergeordnete Organisationseinheit haben.");
        }

        var currentParentId = requestedParentDepartmentId;
        Set<Integer> visited = new HashSet<>();

        while (currentParentId != null) {
            if (!visited.add(currentParentId)) {
                throw ResponseException.badRequest("Die Hierarchie der Organisationseinheiten enthält einen Zyklus.");
            }

            if (departmentId != null && currentParentId.equals(departmentId)) {
                throw ResponseException.badRequest("Die ausgewählte übergeordnete Organisationseinheit befindet sich in der Hierarchie unterhalb der zu verschiebenden Organisationseinheit.");
            }

            var currentParent = departmentRepository
                    .findById(currentParentId)
                    .orElseThrow(() -> ResponseException.badRequest("Die ausgewählte übergeordnete Organisationseinheit existiert nicht."));

            currentParentId = currentParent.getParentDepartmentId();
        }
    }

    @Override
    public void performDelete(@Nonnull DepartmentEntity department) throws ResponseException {
        // TODO: Check if this department still has processes and prevent the deletion if so.

        departmentRepository
                .delete(department);
    }

    /**
     * @deprecated Use shadowed departments
     */
    public ThemeEntity getDepartmentTheme(DepartmentEntity department) {
        if (department.getThemeId() != null) {
            var departmentTheme =  themeRepository
                    .findById(department.getThemeId())
                    .orElse(null);
            if (departmentTheme != null) {
                return departmentTheme;
            }
        }
        return systemService
                .retrieveDefaultTheme();
    }
}
