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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class DataObjectItemService implements EntityService<DataObjectItemEntity, DataObjectItemEntityId> {
    public static final String ID_GEN_UUID = "__UUID__";
    public static final String ID_GEN_SERIAL = "__SERIAL__";
    public static final String ID_GEN_CUSTOM = "__CUSTOM__";

    public static final String ID_GEN_PART_YEAR = "%Y";
    public static final String ID_GEN_PART_MONTH = "%M";
    public static final String ID_GEN_PART_DAY = "%D";

    public static final String ID_GEN_INC_PATTERN = "%I[1-9]";
    public static final String ID_GEN_FLUFF_PATTERN = "([a-zA-Z0-9_\\\\.-]|(%Y)|(%M)|(%D))+";
    public static final Pattern ID_GEN_INC_START_PATTERN = Pattern.compile("^(" + ID_GEN_INC_PATTERN + ")" + ID_GEN_FLUFF_PATTERN);
    public static final Pattern ID_GEN_INC_END_PATTERN = Pattern.compile(ID_GEN_FLUFF_PATTERN + "(" + ID_GEN_INC_PATTERN + ")$");

    public static final String ID_FIELD_NAME = "$id";

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
        switch (schema.getIdGen()) {
            case ID_GEN_UUID -> {
                id = UUID.randomUUID().toString();
            }
            case ID_GEN_SERIAL -> {
                var maxId = dataObjectItemRepository.getMaxIdBySchemaKey(entity.getSchemaKey());
                if (maxId == null) {
                    maxId = 0;
                }
                id = String.valueOf(maxId + 1);
            }
            case ID_GEN_CUSTOM -> {
                var _id = getCustomObjectId(entity);
                id = String.valueOf(_id);
            }
            default -> {
                var now = ZonedDateTime.now(ZoneId.of("Europe/Berlin"));

                var startMatcher = ID_GEN_INC_START_PATTERN.matcher(schema.getIdGen());
                var endMatcher = ID_GEN_INC_END_PATTERN.matcher(schema.getIdGen());

                int padding;
                if (startMatcher.matches()) {
                    padding = Character
                            .getNumericValue(startMatcher
                                    .group(1)
                                    .charAt(2));
                } else if (endMatcher.matches()) {
                    padding = Character
                            .getNumericValue(endMatcher
                                    .group(endMatcher.groupCount())
                                    .charAt(2));
                } else {
                    throw ResponseException.badRequest("ID generation pattern must contain an increment part (%I{n}) at the start or end: " + schema.getIdGen());
                }

                var fluff = schema
                        .getIdGen()
                        .replace(ID_GEN_PART_YEAR, String.valueOf(now.getYear()))
                        .replace(ID_GEN_PART_MONTH, String.format("%02d", now.getMonthValue()))
                        .replace(ID_GEN_PART_DAY, String.format("%02d", now.getDayOfMonth()))
                        .replaceFirst(ID_GEN_INC_PATTERN, "");

                var currentMaxId = dataObjectItemRepository
                        .getMaxFluffedIdBySchemaKey(fluff, entity.getSchemaKey());
                if (currentMaxId == null) {
                    currentMaxId = 0;
                }

                id = schema
                        .getIdGen()
                        .replace(ID_GEN_PART_YEAR, String.valueOf(now.getYear()))
                        .replace(ID_GEN_PART_MONTH, String.format("%02d", now.getMonthValue()))
                        .replace(ID_GEN_PART_DAY, String.format("%02d", now.getDayOfMonth()))
                        .replaceFirst(ID_GEN_INC_PATTERN, String.format("%0" + padding + "d", currentMaxId + 1));
            }
        }

        var idCheck = dataObjectItemRepository
                .existsById(new DataObjectItemEntityId(entity.getSchemaKey(), id));
        if (idCheck) {
            throw ResponseException
                    .badRequest("Es existiert bereits ein Datenobjekt mit der ID '" + id + "' für das Schema '" + entity.getSchemaKey() + "'");
        }

        entity.setId(id);

        var derivedObjectItemData = deriveDataObjectItemData(entity, schema);
        entity.setData(derivedObjectItemData);

        return dataObjectItemRepository
                .save(entity);
    }

    @Nonnull
    private static Object getCustomObjectId(@Nonnull DataObjectItemEntity entity) throws ResponseException {
        var _id = entity
                .getData()
                .get(ID_FIELD_NAME);

        if (_id == null) {
            throw ResponseException
                    .badRequest("Für die ID-Generierungsmethode '__CUSTOM__' muss im Datenobjekt ein Feld „" + ID_FIELD_NAME + "“ mit dem gewünschten ID-Wert übergeben werden.");
        }

        return _id;
    }

    @Override
    public void performDelete(@Nonnull DataObjectItemEntity entity) throws ResponseException {
        entity.setDeleted(LocalDateTime.now());
        dataObjectItemRepository.save(entity);
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
        if (existingEntity.getDeleted() != null) {
            throw ResponseException.notFound();
        }

        var schema = dataObjectSchemaRepository
                .findById(entity.getSchemaKey())
                .orElseThrow(ResponseException::badRequest);

        if (ID_GEN_CUSTOM.equals(schema.getIdGen())) {
            getCustomObjectId(entity);
        }

        var derivedObjectItemData = deriveDataObjectItemData(entity, schema);
        derivedObjectItemData.put(ID_FIELD_NAME, existingEntity.getId());
        existingEntity.setData(derivedObjectItemData);

        return dataObjectItemRepository
                .save(existingEntity);
    }

    @Nonnull
    private Map<String, Object> deriveDataObjectItemData(@Nonnull DataObjectItemEntity entity,
                                                         @Nonnull DataObjectSchemaEntity schema) throws ResponseException {
        var entityElementData = ElementData
                .fromValueMap(schema.getSchema(), entity.getData());
        var edo = new ElementDerivationOptions();
        var edr = new ElementDerivationRequest()
                .setElement(schema.getSchema())
                .setElementData(entityElementData)
                .setOptions(edo);
        var derivedData = elementDerivationService.derive(edr);

        if (derivedData.hasAnyError()) {
            throw ResponseException
                    .badRequest(derivedData);
        }

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
