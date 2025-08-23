package de.aivot.GoverBackend.dataObject.services;

import de.aivot.GoverBackend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.GoverBackend.dataObject.repositories.DataObjectSchemaRepository;
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
import java.util.regex.Pattern;

@Service
public class DataObjectSchemaService implements EntityService<DataObjectSchemaEntity, String> {
    private final DataObjectSchemaRepository dataObjectRepository;

    private static final String ID_GEN_PATTERN = "^(__UUID__|([a-zA-Z0-9\\-_\\.]|%Y|%M|%D|%I)+)$";

    @Autowired
    public DataObjectSchemaService(
            DataObjectSchemaRepository dataObjectRepository
    ) {
        this.dataObjectRepository = dataObjectRepository;
    }

    @Nonnull
    @Override
    public DataObjectSchemaEntity create(@Nonnull DataObjectSchemaEntity entity) throws ResponseException {
        var pattern = Pattern.compile(ID_GEN_PATTERN);
        if (!pattern.matcher(entity.getIdGen()).matches()) {
            throw ResponseException.badRequest("Invalid ID generation pattern: " + entity.getIdGen());
        }
        return dataObjectRepository.save(entity);
    }

    @Override
    public void performDelete(@Nonnull DataObjectSchemaEntity entity) throws ResponseException {
        dataObjectRepository.delete(entity);
    }

    @Nonnull
    @Override
    public Page<DataObjectSchemaEntity> performList(@Nonnull Pageable pageable, @Nullable Specification<DataObjectSchemaEntity> specification, Filter<DataObjectSchemaEntity> filter) {
        return dataObjectRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public DataObjectSchemaEntity performUpdate(@Nonnull String key, @Nonnull DataObjectSchemaEntity entity, @Nonnull DataObjectSchemaEntity existingEntity) throws ResponseException {
        existingEntity.setName(entity.getName());
        existingEntity.setDescription(entity.getDescription());
        existingEntity.setSchema(entity.getSchema());
        return dataObjectRepository.save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<DataObjectSchemaEntity> retrieve(@Nonnull String key) {
        return dataObjectRepository.findById(key);
    }

    @Nonnull
    @Override
    public Optional<DataObjectSchemaEntity> retrieve(@Nonnull Specification<DataObjectSchemaEntity> specification) {
        return dataObjectRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull String key) {
        return dataObjectRepository.existsById(key);
    }

    @Override
    public boolean exists(@Nonnull Specification<DataObjectSchemaEntity> specification) {
        return dataObjectRepository.exists(specification);
    }
}
