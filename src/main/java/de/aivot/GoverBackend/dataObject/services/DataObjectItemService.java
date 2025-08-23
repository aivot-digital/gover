package de.aivot.GoverBackend.dataObject.services;

import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntity;
import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntityId;
import de.aivot.GoverBackend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.GoverBackend.dataObject.repositories.DataObjectItemRepository;
import de.aivot.GoverBackend.dataObject.repositories.DataObjectSchemaRepository;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DataObjectItemService implements EntityService<DataObjectItemEntity, DataObjectItemEntityId> {
    public static final String ID_GEN_UUID = "__UUID__";

    public static final String ID_GEN_PART_YEAR = "%Y";
    public static final String ID_GEN_PART_MONTH = "%M";
    public static final String ID_GEN_PART_DAY = "%D";
    public static final String ID_GEN_PART_INC = "%I";

    private final DataObjectItemRepository dataObjectItemRepository;
    private final DataObjectSchemaRepository dataObjectSchemaRepository;
    private final ElementDerivationService elementDerivationService;

    @Autowired
    public DataObjectItemService(
            DataObjectItemRepository dataObjectItemRepository,
            DataObjectSchemaRepository dataObjectSchemaRepository,
            ElementDerivationService elementDerivationService
    ) {
        this.dataObjectItemRepository = dataObjectItemRepository;
        this.dataObjectSchemaRepository = dataObjectSchemaRepository;
        this.elementDerivationService = elementDerivationService;
    }

    @Nonnull
    @Override
    public DataObjectItemEntity create(@Nonnull DataObjectItemEntity entity) throws ResponseException {
        var schema = dataObjectSchemaRepository
                .findById(entity.getSchemaKey())
                .orElseThrow(ResponseException::badRequest);

        String id;
        if (ID_GEN_UUID.equals(schema.getIdGen())) {
            id = UUID.randomUUID().toString();
        } else {
            var now = ZonedDateTime.now(ZoneId.of("Europe/Berlin"));

            var maxId = dataObjectItemRepository.getMaxIdBySchemaKey(entity.getSchemaKey(), schema.getIdGen().split("%I"));
            if (maxId == null) {
                maxId = 0;
            }

            id = schema.getIdGen()
                    .replace(ID_GEN_PART_YEAR, String.valueOf(now.getYear()))
                    .replace(ID_GEN_PART_MONTH, String.format("%02d", now.getMonthValue()))
                    .replace(ID_GEN_PART_DAY, String.format("%02d", now.getDayOfMonth()))
                    .replace(ID_GEN_PART_INC, String.format("%02d", maxId + 1));
        }

        entity.setId(id);

        var derivedObjectItemData = deriveDataObjectItemData(entity, schema);
        entity.setData(derivedObjectItemData);

        return dataObjectItemRepository
                .save(entity);
    }

    @Override
    public void performDelete(@Nonnull DataObjectItemEntity entity) throws ResponseException {
        dataObjectItemRepository
                .delete(entity);
    }

    @Nullable
    @Override
    public Page<DataObjectItemEntity> performList(@Nonnull Pageable pageable,
                                                  @Nullable Specification<DataObjectItemEntity> specification,
                                                  @Nullable Filter<DataObjectItemEntity> filter) throws ResponseException {
        return dataObjectItemRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public DataObjectItemEntity performUpdate(@Nonnull DataObjectItemEntityId id,
                                              @Nonnull DataObjectItemEntity entity,
                                              @Nonnull DataObjectItemEntity existingEntity) throws ResponseException {
        var schema = dataObjectSchemaRepository
                .findById(entity.getSchemaKey())
                .orElseThrow(ResponseException::badRequest);

        var derivedObjectItemData = deriveDataObjectItemData(entity, schema);
        existingEntity.setData(derivedObjectItemData);

        return dataObjectItemRepository
                .save(existingEntity);
    }

    @Nonnull
    private Map<String, Object> deriveDataObjectItemData(@Nonnull DataObjectItemEntity entity,
                                                         @Nonnull DataObjectSchemaEntity schema) {
        var entityElementData = ElementData
                .fromValueMap(schema.getSchema(), entity.getData());
        var edo = new ElementDerivationOptions();
        var edr = new ElementDerivationRequest()
                .setElement(schema.getSchema())
                .setElementData(entityElementData)
                .setOptions(edo);
        var derivedData = elementDerivationService.derive(edr);
        return ElementData.toValueMap(schema.getSchema(), derivedData);
    }

    @Nonnull
    @Override
    public Optional<DataObjectItemEntity> retrieve(@Nonnull DataObjectItemEntityId id) throws ResponseException {
        return dataObjectItemRepository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<DataObjectItemEntity> retrieve(@Nonnull Specification<DataObjectItemEntity> specification) throws ResponseException {
        return dataObjectItemRepository
                .findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull DataObjectItemEntityId id) {
        return dataObjectItemRepository
                .existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<DataObjectItemEntity> specification) {
        return dataObjectItemRepository
                .exists(specification);
    }
}
