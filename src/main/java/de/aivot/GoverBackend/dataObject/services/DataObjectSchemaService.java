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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
                    throw ResponseException.badRequest("Der gewählte ID-Typ setzt ein Element mit der ID „" + DataObjectItemService.ID_FIELD_NAME + "“ voraus. Stellen Sie sicher, dass das Feld auf der obersten Ebene des Datenmodells definiert ist.");
                }

                var idChild = children
                        .stream()
                        .filter(c -> c.getId().equals(DataObjectItemService.ID_FIELD_NAME))
                        .findFirst();

                if (idChild.isEmpty()) {
                    throw ResponseException.badRequest("Der gewählte ID-Typ setzt ein Element mit der ID „" + DataObjectItemService.ID_FIELD_NAME + "“ voraus. Stellen Sie sicher, dass das Feld auf der obersten Ebene des Datenmodells definiert ist.");
                }

                var idChildElement = idChild.get();
                if (idChildElement instanceof TextField textField) {
                    if (!Boolean.TRUE.equals(textField.getRequired())) {
                        throw ResponseException.badRequest("Der gewählte ID-Typ setzt voraus, dass das Element mit der ID „" + DataObjectItemService.ID_FIELD_NAME + "“ ein Pflichtfeld ist.");
                    }
                } else {
                    throw ResponseException.badRequest("Der gewählte ID-Typ setzt voraus, dass das Element mit der ID „" + DataObjectItemService.ID_FIELD_NAME + "“ ein Textfeld ist.");
                }
                break;
            default:
                var startPatternPresent = DataObjectItemService.ID_GEN_INC_START_PATTERN.matcher(entity.getIdGen()).matches();
                var endPatternPresent = DataObjectItemService.ID_GEN_INC_END_PATTERN.matcher(entity.getIdGen()).matches();

                if (!startPatternPresent && !endPatternPresent) {
                    throw ResponseException.badRequest("Das Format des gewählten ID-Typs ist ungültig. Bitte stellen Sie sicher, dass der ID-Typ mit „%I[0-9]“ beginnt oder endet.");
                }
                break;
        }
    }
}
