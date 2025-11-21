package de.aivot.GoverBackend.department.services;

import de.aivot.GoverBackend.department.entities.OrganizationalUnitEntity;
import de.aivot.GoverBackend.department.repositories.OrganizationalUnitRepository;
import de.aivot.GoverBackend.form.filters.FormFilter;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.system.services.SystemService;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import de.aivot.GoverBackend.theme.repositories.ThemeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OrganizationalUnitService implements EntityService<OrganizationalUnitEntity, Integer> {
    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final FormRepository formRepository;
    private final ThemeRepository themeRepository;
    private final SystemService systemService;

    @Autowired
    public OrganizationalUnitService(OrganizationalUnitRepository organizationalUnitRepository,
                                     FormRepository formRepository,
                                     ThemeRepository themeRepository, SystemService systemService) {
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.formRepository = formRepository;
        this.themeRepository = themeRepository;
        this.systemService = systemService;
    }

    @Nonnull
    @Override
    public OrganizationalUnitEntity create(@Nonnull OrganizationalUnitEntity entity) throws ResponseException {
        entity.setId(null);
        return organizationalUnitRepository
                .save(entity);
    }

    @Nonnull
    @Override
    public Page<OrganizationalUnitEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<OrganizationalUnitEntity> specification,
            Filter<OrganizationalUnitEntity> filter) {
        return organizationalUnitRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<OrganizationalUnitEntity> retrieve(@Nonnull Integer id) {
        return organizationalUnitRepository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<OrganizationalUnitEntity> retrieve(
            @Nonnull Specification<OrganizationalUnitEntity> specification
    ) {
        return organizationalUnitRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return organizationalUnitRepository.existsById(id);
    }

    @Override
    public boolean exists(
            @Nonnull Specification<OrganizationalUnitEntity> specification
    ) {
        return organizationalUnitRepository.exists(specification);
    }

    @Nonnull
    @Override
    public OrganizationalUnitEntity performUpdate(
            @Nonnull Integer id,
            @Nonnull OrganizationalUnitEntity entity,
            @Nonnull OrganizationalUnitEntity existingDepartment
    ) throws ResponseException {
        entity.setId(existingDepartment.getId());
        entity.setCreated(existingDepartment.getCreated());
        entity.setUpdated(LocalDateTime.now());

        var themeId = entity.getThemeId();
        if (themeId != null) {
            var themeExists = themeRepository.existsById(themeId);
            if (!themeExists) {
                entity.setThemeId(null);
            }
        }

        return organizationalUnitRepository
                .save(entity);
    }

    @Override
    public void performDelete(@Nonnull OrganizationalUnitEntity department) throws ResponseException {
        var specDevDepartment = FormFilter
                .create()
                .setDevelopingDepartmentId(department.getId())
                .build();

        var specManDepartment = FormFilter
                .create()
                .setManagingDepartmentId(department.getId())
                .build();

        var specRespDepartment = FormFilter
                .create()
                .setResponsibleDepartmentId(department.getId())
                .build();

        var spec = specDevDepartment
                .or(specManDepartment)
                .or(specRespDepartment);

        if (formRepository.exists(spec)) {
            throw new ResponseException(HttpStatus.CONFLICT, "Der Fachbereich kann nicht gelöscht werden, da noch Formulare zugewiesen sind.");
        }

        organizationalUnitRepository
                .delete(department);
    }

    public ThemeEntity getDepartmentTheme(OrganizationalUnitEntity department) {
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
