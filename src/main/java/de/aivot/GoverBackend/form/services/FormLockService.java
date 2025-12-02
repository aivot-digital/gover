package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.form.cache.entities.FormLockCacheEntity;
import de.aivot.GoverBackend.form.cache.repositories.FormLockCacheRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Service
public class FormLockService implements EntityService<FormLockCacheEntity, Integer> {
    private final FormLockCacheRepository formLockCacheRepository;

    @Autowired
    public FormLockService(FormLockCacheRepository formLockCacheRepository) {
        this.formLockCacheRepository = formLockCacheRepository;
    }

    @Nonnull
    @Override
    public FormLockCacheEntity create(@Nonnull FormLockCacheEntity entity) throws ResponseException {
        return formLockCacheRepository.save(entity);
    }

    @Nonnull
    @Override
    public Optional<FormLockCacheEntity> retrieve(@Nonnull Integer id) {
        return formLockCacheRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<FormLockCacheEntity> retrieve(@Nonnull Specification<FormLockCacheEntity> specification) {
        return Optional.empty();
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return formLockCacheRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<FormLockCacheEntity> specification) {
        return false;
    }

    @Override
    public void performDelete(@Nonnull FormLockCacheEntity entity) throws ResponseException {
        formLockCacheRepository.delete(entity);
    }
    
    @Nonnull
    @Override
    public Page<FormLockCacheEntity> performList(@Nonnull Pageable pageable, @Nullable Specification<FormLockCacheEntity> specification, Filter<FormLockCacheEntity> filter) {
        return formLockCacheRepository.findAll(pageable);
    }

    @Nonnull
    @Override
    public FormLockCacheEntity performUpdate(@Nonnull Integer id, @Nonnull FormLockCacheEntity entity, @Nonnull FormLockCacheEntity existingEntity) throws ResponseException {
        return formLockCacheRepository.save(entity);
    }

    public void checkFormLock(@Nonnull Integer formId, @Nonnull UserEntity accessingUser) throws ResponseException {
        var existingFormLock = retrieve(formId);
        if (existingFormLock.isPresent()) {
            var formLockedByUserId = existingFormLock.get().getUserId();
            if (!accessingUser.hasId(formLockedByUserId)) {
                throw ResponseException
                        .locked("Das Formular ist von einer anderen Mitarbeiter:in gesperrt.");
            }
        }
    }
}
