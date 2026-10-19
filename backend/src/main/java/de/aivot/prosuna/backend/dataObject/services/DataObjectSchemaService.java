package de.aivot.prosuna.backend.dataObject.services;

import de.aivot.prosuna.backend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.prosuna.backend.dataObject.repositories.DataObjectSchemaRepository;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

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
        validateSchemaConfig(entity.getIdGen(), entity.getSchema());

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
        validateSchemaConfig(existingEntity.getIdGen(), entity.getSchema());

        existingEntity.setName(entity.getName());
        existingEntity.setDescription(entity.getDescription());
        existingEntity.setSchema(entity.getSchema());
        existingEntity.setDisplayFields(entity.getDisplayFields());

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

    private void validateSchemaConfig(@Nonnull String idGen, @Nullable GroupLayoutElement schema) throws ResponseException {
        if (schema == null) {
            throw ResponseException.badRequest("Bitte legen Sie ein Datenschema mit mindestens einem Datenfeld fest.");
        }
        if (!containsDataField(schema)) {
            throw ResponseException.badRequest("Das Datenschema muss mindestens ein Datenfeld enthalten.");
        }

        switch (idGen) {
            case DataObjectItemService.ID_GEN_UUID:
            case DataObjectItemService.ID_GEN_SERIAL:
                break;
            case DataObjectItemService.ID_GEN_CUSTOM:
                var children = schema.getChildren();

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
                if (idChildElement instanceof TextInputElement textField) {
                    if (!Boolean.TRUE.equals(textField.getRequired())) {
                        throw ResponseException.badRequest("Der gewählte ID-Typ setzt voraus, dass das Element mit der ID „" + DataObjectItemService.ID_FIELD_NAME + "“ ein Pflichtfeld ist.");
                    }
                } else {
                    throw ResponseException.badRequest("Der gewählte ID-Typ setzt voraus, dass das Element mit der ID „" + DataObjectItemService.ID_FIELD_NAME + "“ ein Textfeld ist.");
                }
                break;
            default:
                var startPatternPresent = DataObjectItemService.ID_GEN_INC_START_PATTERN.matcher(idGen).matches();
                var endPatternPresent = DataObjectItemService.ID_GEN_INC_END_PATTERN.matcher(idGen).matches();

                if (!startPatternPresent && !endPatternPresent) {
                    throw ResponseException.badRequest("Das Format des gewählten ID-Typs ist ungültig. Bitte stellen Sie sicher, dass der ID-Typ mit „%I[0-9]“ beginnt oder endet.");
                }
                break;
        }
    }

    private boolean containsDataField(@Nullable BaseElement element) {
        // Repeating containers are inputs too, but an empty container does not define a data field.
        if (element instanceof LayoutElement<?> layout) {
            return layout.getChildren().stream().anyMatch(this::containsDataField);
        }
        return element instanceof BaseInputElement<?>;
    }
}
