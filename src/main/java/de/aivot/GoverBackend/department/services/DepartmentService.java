package de.aivot.GoverBackend.department.services;

import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.form.filters.FormFilter;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
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
public class DepartmentService implements EntityService<DepartmentEntity, Integer> {
    private final DepartmentRepository departmentRepository;
    private final FormRepository formRepository;

    @Autowired
    public DepartmentService(
            DepartmentRepository departmentRepository,
            FormRepository formRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.formRepository = formRepository;
    }

    @Nonnull
    @Override
    public DepartmentEntity create(@Nonnull DepartmentEntity entity) throws ResponseException {
        entity.setId(null);
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
        entity.setId(existingDepartment.getId());
        entity.setCreated(existingDepartment.getCreated());
        entity.setUpdated(LocalDateTime.now());
        return departmentRepository
                .save(entity);
    }

    @Override
    public void performDelete(@Nonnull DepartmentEntity department) throws ResponseException {
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

        departmentRepository
                .delete(department);
    }
}
