package de.aivot.GoverBackend.dataObject.services;

import de.aivot.GoverBackend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.GoverBackend.dataObject.repositories.DataObjectSchemaRepository;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextField;
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
public class DataObjectSchemaService implements EntityService<DataObjectSchemaEntity, String> {
    private final DataObjectSchemaRepository dataObjectRepository;


    @Autowired
    public DataObjectSchemaService(
            DataObjectSchemaRepository dataObjectRepository
    ) {
        this.dataObjectRepository = dataObjectRepository;
    }

    @Nonnull
    @Override
    public DataObjectSchemaEntity create(@Nonnull DataObjectSchemaEntity entity) throws ResponseException {
        validateSchemaConfig(entity);

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
        existingEntity.setDisplayFields(entity.getDisplayFields());

        validateSchemaConfig(existingEntity);

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

    private void validateSchemaConfig(@Nonnull DataObjectSchemaEntity entity) throws ResponseException {
        switch (entity.getIdGen()) {
            case DataObjectItemService.ID_GEN_UUID:
            case DataObjectItemService.ID_GEN_SERIAL:
                break;
            case DataObjectItemService.ID_GEN_CUSTOM:
                var children = entity
                        .getSchema()
                        .getChildren();
                if (children == null || children.isEmpty()) {
                    throw ResponseException.badRequest("Custom ID generation requires a schema with at least one child.");
                }
                var idChild = children
                        .stream()
                        .filter(c -> c.getId().equals("$id"))
                        .findFirst();
                if (idChild.isEmpty()) {
                    throw ResponseException.badRequest("Custom ID generation requires a '$id' field in the schema.");
                }
                var idChildElement = idChild.get();
                if (idChildElement instanceof TextField textField) {
                    if (!Boolean.TRUE.equals(textField.getRequired())) {
                        throw ResponseException.badRequest("Custom ID generation requires the '$id' field to be required.");
                    }
                } else {
                    throw ResponseException.badRequest("Custom ID generation requires the '$id' field to be a TextField.");
                }
                break;
            default:
                var startPatternPresent = DataObjectItemService.ID_GEN_INC_START_PATTERN.matcher(entity.getIdGen()).matches();
                var endPatternPresent = DataObjectItemService.ID_GEN_INC_END_PATTERN.matcher(entity.getIdGen()).matches();

                if (!startPatternPresent && !endPatternPresent) {
                    throw ResponseException.badRequest("Invalid ID generation pattern. It must contain an increment pattern at the start or the end.");
                }
        }
    }
}
