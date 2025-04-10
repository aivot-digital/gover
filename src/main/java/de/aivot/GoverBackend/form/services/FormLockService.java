package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.form.cache.entities.FormLock;
import de.aivot.GoverBackend.form.cache.repositories.FormLockRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Service
public class FormLockService implements EntityService<FormLock, Integer> {
    private final FormLockRepository formLockRepository;

    @Autowired
    public FormLockService(FormLockRepository formLockRepository) {
        this.formLockRepository = formLockRepository;
    }

    @Nonnull
    @Override
    public FormLock create(@Nonnull FormLock entity) throws ResponseException {
        return formLockRepository.save(entity);
    }

    @Nonnull
    @Override
    public Optional<FormLock> retrieve(@Nonnull Integer id) {
        return formLockRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<FormLock> retrieve(@Nonnull Specification<FormLock> specification) {
        return Optional.empty();
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return formLockRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<FormLock> specification) {
        return false;
    }

    @Override
    public void performDelete(@Nonnull FormLock entity) throws ResponseException {
        formLockRepository.delete(entity);
    }
    
    @Nonnull
    @Override
    public Page<FormLock> performList(@Nonnull Pageable pageable, @Nullable Specification<FormLock> specification, Filter<FormLock> filter) {
        return formLockRepository.findAll(pageable);
    }

    @Nonnull
    @Override
    public FormLock performUpdate(@Nonnull Integer id, @Nonnull FormLock entity, @Nonnull FormLock existingEntity) throws ResponseException {
        return formLockRepository.save(entity);
    }
}
