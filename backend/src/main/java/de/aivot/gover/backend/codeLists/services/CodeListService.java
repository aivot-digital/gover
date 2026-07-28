package de.aivot.gover.backend.codeLists.services;

import de.aivot.gover.backend.asset.services.AssetService;
import de.aivot.gover.backend.codeLists.entities.CodeListEntity;
import de.aivot.gover.backend.codeLists.entities.CodeListItemEntity;
import de.aivot.gover.backend.codeLists.entities.VCodeListItemEntity;
import de.aivot.gover.backend.codeLists.enums.CodeListSourceType;
import de.aivot.gover.backend.codeLists.enums.CodeListStatus;
import de.aivot.gover.backend.codeLists.repositories.CodeListItemRepository;
import de.aivot.gover.backend.codeLists.repositories.CodeListRepository;
import de.aivot.gover.backend.codeLists.repositories.VCodeListItemRepository;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.lib.services.EntityService;
import de.aivot.gover.backend.storage.services.StorageService;
import de.aivot.gover.backend.xrepository.models.XRepositoryCodeList;
import de.aivot.gover.backend.xrepository.services.XRepositoryCodeListService;
import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class CodeListService implements EntityService<CodeListEntity, String> {
    private final CodeListRepository codeListRepository;
    private final CodeListItemRepository codeListItemRepository;
    private final VCodeListItemRepository vCodeListItemRepository;
    private final XRepositoryCodeListService xRepositoryCodeListService;
    private final AssetService assetService;
    private final StorageService storageService;


    @Autowired
    public CodeListService(
            CodeListRepository codeListRepository,
            CodeListItemRepository codeListItemRepository,
            VCodeListItemRepository vCodeListItemRepository,
            XRepositoryCodeListService xRepositoryCodeListService,
            AssetService assetService,
            StorageService storageService
    ) {
        this.codeListRepository = codeListRepository;
        this.codeListItemRepository = codeListItemRepository;
        this.vCodeListItemRepository = vCodeListItemRepository;
        this.xRepositoryCodeListService = xRepositoryCodeListService;
        this.assetService = assetService;
        this.storageService = storageService;
    }

    @Nonnull
    @Override
    public CodeListEntity create(@Nonnull CodeListEntity entity) throws ResponseException {
        entity.setId(null);
        normalizeCodeList(entity);
        return codeListRepository.save(entity);
    }

    @Override
    public void performDelete(@Nonnull CodeListEntity entity) throws ResponseException {
        codeListRepository.delete(entity);
    }

    @Nonnull
    @Override
    public Page<CodeListEntity> performList(@Nonnull Pageable pageable, @Nullable Specification<CodeListEntity> specification, Filter<CodeListEntity> filter) {
        return codeListRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public CodeListEntity performUpdate(@Nonnull String key,
                                        @Nonnull CodeListEntity entity,
                                        @Nonnull CodeListEntity existingEntity) throws ResponseException {
        existingEntity.setSourceType(entity.getSourceType());
        existingEntity.setSourceRef(entity.getSourceRef());
        existingEntity.setName(entity.getName());
        existingEntity.setDescription(entity.getDescription());
        existingEntity.setColumns(entity.getColumns());
        existingEntity.setValueColumnIndex(entity.getValueColumnIndex());
        existingEntity.setLabelColumnIndex(entity.getLabelColumnIndex());
        normalizeCodeList(existingEntity);
        existingEntity
                .setStatus(isSyncable(existingEntity) ? CodeListStatus.SyncPending : CodeListStatus.Synced)
                .setStatusMessage(null);
        return codeListRepository.save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<CodeListEntity> retrieve(@Nonnull String key) {
        return codeListRepository.findById(key);
    }

    @Nonnull
    @Override
    public Optional<CodeListEntity> retrieve(@Nonnull Specification<CodeListEntity> specification) {
        return codeListRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull String key) {
        return codeListRepository.existsById(key);
    }

    @Override
    public boolean exists(@Nonnull Specification<CodeListEntity> specification) {
        return codeListRepository.exists(specification);
    }

    public void syncCodeList(String codeListKey, boolean keepOutdated) {
        var codeList = codeListRepository
                .findById(codeListKey)
                .orElse(null);

        if (codeList == null) {
            return;
        }

        if (!isSyncable(codeList)) {
            return;
        }

        markSyncing(codeList);

        try {
            switch (codeList.getSourceType()) {
                case XRepository -> syncXRepository(codeList, keepOutdated);
                case Asset -> syncAsset(codeList, keepOutdated);
                case Manual, Plugin -> {
                    return;
                }
            }
            markSynced(codeList);
        } catch (Exception e) {
            markSyncFailed(codeList, e.getMessage());
        }
    }

    @Nonnull
    public byte[] exportCSV(@Nonnull String codeListKey) throws ResponseException {
        var codeList = requireCodeList(codeListKey);
        var codeListId = requireInternalId(codeList);
        var items = vCodeListItemRepository.findAllByCodeListIdOrderByIdAsc(codeListId);
        var outputStream = new ByteArrayOutputStream();

        try (var csv = CsvWriter.builder().build(outputStream, StandardCharsets.UTF_8)) {
            csv.writeRecord(codeList.getColumns());
            for (var item : items) {
                csv.writeRecord(item.getColumns());
            }
        } catch (IOException | UncheckedIOException e) {
            throw ResponseException.internalServerError(e, "Die CSV-Datei konnte nicht erzeugt werden: %s", e.getMessage());
        }

        return outputStream.toByteArray();
    }

    @Nonnull
    @Transactional
    public CodeListEntity importCSV(@Nonnull String codeListKey,
                                    @Nonnull InputStream inputStream) throws ResponseException {
        var codeList = requireManualCodeList(codeListKey);

        List<CodeListItemEntity> importedItems;
        try (var csv = CsvReader.builder()
                .detectBomHeader(true)
                .ofCsvRecord(inputStream)
        ) {
            importedItems = extractCsvItems(codeList, csv);
        } catch (CsvParseException | UncheckedIOException e) {
            throw ResponseException.badRequest("Die CSV-Datei konnte nicht gelesen werden: " + e.getMessage(), e);
        } catch (IOException e) {
            throw ResponseException.internalServerError(e, "Die CSV-Datei konnte nicht geschlossen werden: %s", e.getMessage());
        }

        validateUniqueItemValues(codeList, importedItems);
        replaceItems(codeList, importedItems);
        return codeListRepository.save(codeList);
    }

    @Nonnull
    public List<String> getXRepositoryColumns(@Nonnull String urn) throws ResponseException {
        return extractColumnRefs(xRepositoryCodeListService.getCodeList(urn));
    }

    @Nonnull
    public List<String> getAssetColumns(@Nonnull UUID assetKey) throws ResponseException {
        var asset = assetService
                .retrieve(assetKey)
                .orElseThrow(ResponseException::notFound);

        try (
                var inputStream = storageService.getDocumentContent(asset.getStorageProviderId(), asset.getStoragePathFromRoot());
                var csv = CsvReader.builder()
                        .detectBomHeader(true)
                        .ofCsvRecord(inputStream)
        ) {
            return extractCsvHeader(csv);
        } catch (CsvParseException | UncheckedIOException e) {
            throw ResponseException.badRequest("Die CSV-Datei konnte nicht gelesen werden: " + e.getMessage(), e);
        } catch (IOException e) {
            throw ResponseException.internalServerError(e, "Die CSV-Datei konnte nicht geschlossen werden: %s", e.getMessage());
        }
    }

    @Nonnull
    public Page<VCodeListItemEntity> listItems(@Nonnull String codeListKey, @Nonnull Pageable pageable) throws ResponseException {
        var codeListId = requireInternalId(requireCodeList(codeListKey));
        return vCodeListItemRepository.findAllByCodeListId(codeListId, pageable);
    }

    @Nonnull
    public List<VCodeListItemEntity> listAllItems(@Nonnull String codeListKey) throws ResponseException {
        var codeListId = requireInternalId(requireCodeList(codeListKey));
        return vCodeListItemRepository.findAllByCodeListIdOrderByIdAsc(codeListId);
    }

    @Nonnull
    public VCodeListItemEntity createItem(@Nonnull String codeListKey, @Nonnull CodeListItemEntity item) throws ResponseException {
        var codeList = requireManualCodeList(codeListKey);
        var codeListId = requireInternalId(codeList);
        item
                .setId(null)
                .setCodeListId(codeListId)
                .setColumns(normalizeItemColumns(codeList, item.getColumns()));

        var saved = codeListItemRepository.save(item);
        return getItem(codeListKey, saved.getId());
    }

    @Nonnull
    public VCodeListItemEntity getItem(@Nonnull String codeListKey, @Nonnull Long itemId) throws ResponseException {
        var codeListId = requireInternalId(requireCodeList(codeListKey));
        return vCodeListItemRepository
                .findByIdAndCodeListId(itemId, codeListId)
                .orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    public VCodeListItemEntity updateItem(@Nonnull String codeListKey,
                                          @Nonnull Long itemId,
                                          @Nonnull CodeListItemEntity item) throws ResponseException {
        var codeList = requireManualCodeList(codeListKey);
        var codeListId = requireInternalId(codeList);
        var existingItem = codeListItemRepository
                .findByIdAndCodeListId(itemId, codeListId)
                .orElseThrow(ResponseException::notFound);

        existingItem.setColumns(normalizeItemColumns(codeList, item.getColumns()));
        codeListItemRepository.save(existingItem);
        return getItem(codeListKey, itemId);
    }

    public void deleteItem(@Nonnull String codeListKey, @Nonnull Long itemId) throws ResponseException {
        var codeList = requireManualCodeList(codeListKey);
        var codeListId = requireInternalId(codeList);
        var existingItem = codeListItemRepository
                .findByIdAndCodeListId(itemId, codeListId)
                .orElseThrow(ResponseException::notFound);
        codeListItemRepository.delete(existingItem);
    }

    private void syncXRepository(CodeListEntity codeListEntity, boolean keepOutdated) throws ResponseException {
        var xRepositoryCodeList = xRepositoryCodeListService.getCodeList(codeListEntity.getSourceRef());
        var columnRefs = extractColumnRefs(xRepositoryCodeList);

        if (codeListEntity.getColumns() == null || codeListEntity.getColumns().isEmpty()) {
            var keyColumnRef = extractKeyColumnRef(xRepositoryCodeList);
            var keyColumnIndex = keyColumnRef == null ? -1 : columnRefs.indexOf(keyColumnRef);
            if (keyColumnIndex >= 0) {
                codeListEntity
                        .setValueColumnIndex(keyColumnIndex)
                        .setLabelColumnIndex(keyColumnIndex);
            }
        }

        codeListEntity.setColumns(columnRefs);
        validateCodeListColumns(codeListEntity);
        codeListRepository.save(codeListEntity);

        var importedItems = extractItems(codeListEntity, xRepositoryCodeList, columnRefs);
        validateUniqueItemValues(codeListEntity, importedItems);

        if (keepOutdated) {
            upsertItems(codeListEntity, importedItems);
        } else {
            replaceItems(codeListEntity, importedItems);
        }
    }

    private void syncAsset(CodeListEntity codeListEntity, boolean keepOutdated) throws ResponseException {
        var assetKey = parseAssetKey(codeListEntity.getSourceRef());
        var asset = assetService
                .retrieve(assetKey)
                .orElseThrow(ResponseException::notFound);

        List<CodeListItemEntity> importedItems;
        try (
                var inputStream = storageService.getDocumentContent(asset.getStorageProviderId(), asset.getStoragePathFromRoot());
                var csv = CsvReader.builder()
                        .detectBomHeader(true)
                        .ofCsvRecord(inputStream)
        ) {
            importedItems = extractCsvItems(codeListEntity, csv);
        } catch (CsvParseException | UncheckedIOException e) {
            throw ResponseException.badRequest("Die CSV-Datei konnte nicht gelesen werden: " + e.getMessage(), e);
        } catch (IOException e) {
            throw ResponseException.internalServerError(e, "Die CSV-Datei konnte nicht geschlossen werden: %s", e.getMessage());
        }

        validateUniqueItemValues(codeListEntity, importedItems);
        codeListRepository.save(codeListEntity);

        if (keepOutdated) {
            upsertItems(codeListEntity, importedItems);
        } else {
            replaceItems(codeListEntity, importedItems);
        }
    }

    private void replaceItems(@Nonnull CodeListEntity codeListEntity,
                              @Nonnull List<CodeListItemEntity> importedItems) throws ResponseException {
        var codeListId = requireInternalId(codeListEntity);
        codeListItemRepository.deleteAllByCodeListId(codeListId);
        codeListItemRepository.saveAll(importedItems);
    }

    private void upsertItems(@Nonnull CodeListEntity codeListEntity,
                             @Nonnull List<CodeListItemEntity> importedItems) throws ResponseException {
        var codeListId = requireInternalId(codeListEntity);
        var valueColumnIndex = codeListEntity.getValueColumnIndex();
        var existingByValue = new HashMap<String, CodeListItemEntity>();

        for (var existingItem : codeListItemRepository.findAllByCodeListId(codeListId)) {
            existingByValue.putIfAbsent(getColumnValue(existingItem, valueColumnIndex), existingItem);
        }

        var itemsToSave = new ArrayList<CodeListItemEntity>();
        for (var importedItem : importedItems) {
            var importedValue = getColumnValue(importedItem, valueColumnIndex);
            var existingItem = existingByValue.get(importedValue);
            if (existingItem == null) {
                itemsToSave.add(importedItem);
            } else {
                existingItem.setColumns(importedItem.getColumns());
                itemsToSave.add(existingItem);
            }
        }

        codeListItemRepository.saveAll(itemsToSave);
    }

    @Nonnull
    private List<String> extractColumnRefs(@Nonnull XRepositoryCodeList xRepositoryCodeList) throws ResponseException {
        if (xRepositoryCodeList.getColumnSet() == null || xRepositoryCodeList.getColumnSet().getColumn() == null) {
            throw ResponseException.internalServerError("Die XRepository-Codeliste enthält keine Spalten.");
        }

        var columnRefs = xRepositoryCodeList
                .getColumnSet()
                .getColumn()
                .stream()
                .map(column -> column.getId() == null ? "" : column.getId())
                .toList();

        if (columnRefs.isEmpty() || columnRefs.stream().anyMatch(String::isBlank)) {
            throw ResponseException.internalServerError("Die XRepository-Codeliste enthält ungültige Spalten.");
        }

        return columnRefs;
    }

    @Nullable
    private String extractKeyColumnRef(@Nonnull XRepositoryCodeList xRepositoryCodeList) {
        if (xRepositoryCodeList.getColumnSet() == null ||
                xRepositoryCodeList.getColumnSet().getKey() == null ||
                xRepositoryCodeList.getColumnSet().getKey().getColumnRef() == null) {
            return null;
        }
        return xRepositoryCodeList.getColumnSet().getKey().getColumnRef().getRef();
    }

    @Nonnull
    private List<CodeListItemEntity> extractItems(@Nonnull CodeListEntity codeListEntity,
                                                  @Nonnull XRepositoryCodeList xRepositoryCodeList,
                                                  @Nonnull List<String> columnRefs) throws ResponseException {
        if (xRepositoryCodeList.getCodeList() == null || xRepositoryCodeList.getCodeList().getRow() == null) {
            return List.of();
        }

        var codeListId = requireInternalId(codeListEntity);
        var items = new ArrayList<CodeListItemEntity>();
        for (var row : xRepositoryCodeList.getCodeList().getRow()) {
            var valuesByColumnRef = new HashMap<String, String>();
            if (row.getValue() != null) {
                for (var value : row.getValue()) {
                    valuesByColumnRef.put(value.getColumnRef(), value.getSimpleValue() == null ? "" : value.getSimpleValue());
                }
            }

            var columns = columnRefs
                    .stream()
                    .map(columnRef -> valuesByColumnRef.getOrDefault(columnRef, ""))
                    .toList();

            items.add(new CodeListItemEntity()
                    .setCodeListId(codeListId)
                    .setColumns(columns));
        }

        return items;
    }

    private void validateUniqueItemValues(@Nonnull CodeListEntity codeListEntity,
                                          @Nonnull List<CodeListItemEntity> items) throws ResponseException {
        var values = new HashSet<String>();
        for (var item : items) {
            var value = getColumnValue(item, codeListEntity.getValueColumnIndex());
            if (!values.add(value)) {
                throw ResponseException.conflict("Die Codeliste enthält den Wert %s mehrfach.", value);
            }
        }
    }

    @Nonnull
    private List<CodeListItemEntity> extractCsvItems(@Nonnull CodeListEntity codeListEntity,
                                                     @Nonnull CsvReader<CsvRecord> csv) throws ResponseException {
        var iterator = csv.iterator();
        codeListEntity.setColumns(extractCsvHeader(iterator));
        normalizeColumnIndexes(codeListEntity);
        validateCodeListColumns(codeListEntity);

        var codeListId = requireInternalId(codeListEntity);
        var items = new ArrayList<CodeListItemEntity>();
        while (iterator.hasNext()) {
            var columns = new ArrayList<>(iterator.next().getFields());
            if (columns.size() != codeListEntity.getColumns().size()) {
                throw ResponseException.badRequest("Die CSV-Datei enthält Zeilen mit unterschiedlicher Spaltenanzahl.");
            }
            items.add(new CodeListItemEntity()
                    .setCodeListId(codeListId)
                    .setColumns(columns));
        }
        return items;
    }

    @Nonnull
    private List<String> extractCsvHeader(@Nonnull CsvReader<CsvRecord> csv) throws ResponseException {
        return extractCsvHeader(csv.iterator());
    }

    @Nonnull
    private List<String> extractCsvHeader(@Nonnull Iterator<CsvRecord> iterator) throws ResponseException {
        if (!iterator.hasNext()) {
            throw ResponseException.badRequest("Die CSV-Datei enthält keine Kopfzeile.");
        }

        return normalizeCsvHeader(iterator.next().getFields());
    }

    @Nonnull
    private List<String> normalizeCsvHeader(@Nonnull List<String> header) throws ResponseException {
        if (header.isEmpty()) {
            throw ResponseException.badRequest("Die CSV-Kopfzeile enthält keine Spalten.");
        }

        var normalizedHeader = new ArrayList<String>();
        for (var i = 0; i < header.size(); i++) {
            var column = header.get(i);
            normalizedHeader.add(column == null || column.isBlank() ? "COL" + i : column);
        }

        var duplicates = new LinkedHashSet<String>();
        var seen = new HashSet<String>();
        for (var column : normalizedHeader) {
            if (!seen.add(column)) {
                duplicates.add(column);
            }
        }
        if (!duplicates.isEmpty()) {
            throw ResponseException.badRequest("Die CSV-Kopfzeile enthält doppelte Spaltennamen: %s", String.join(", ", duplicates));
        }

        return normalizedHeader;
    }

    private void normalizeColumnIndexes(@Nonnull CodeListEntity codeListEntity) {
        var columns = codeListEntity.getColumns();
        if (columns == null || columns.isEmpty()) {
            codeListEntity
                    .setValueColumnIndex(0)
                    .setLabelColumnIndex(0);
            return;
        }

        var lastIndex = columns.size() - 1;
        if (codeListEntity.getValueColumnIndex() == null ||
                codeListEntity.getValueColumnIndex() < 0 ||
                codeListEntity.getValueColumnIndex() > lastIndex) {
            codeListEntity.setValueColumnIndex(0);
        }
        if (codeListEntity.getLabelColumnIndex() == null ||
                codeListEntity.getLabelColumnIndex() < 0 ||
                codeListEntity.getLabelColumnIndex() > lastIndex) {
            codeListEntity.setLabelColumnIndex(Math.min(1, lastIndex));
        }
    }

    @Nonnull
    private UUID parseAssetKey(@Nonnull String sourceRef) throws ResponseException {
        try {
            return UUID.fromString(sourceRef);
        } catch (IllegalArgumentException e) {
            throw ResponseException.badRequest("Die Asset-Referenz der Codeliste ist keine gültige UUID.", e);
        }
    }

    private void normalizeCodeList(@Nonnull CodeListEntity codeListEntity) throws ResponseException {
        if (codeListEntity.getKey() != null) {
            codeListEntity.setKey(codeListEntity.getKey().trim());
        }
        if (codeListEntity.getSourceType() == null) {
            codeListEntity.setSourceType(CodeListSourceType.Manual);
        }
        if (codeListEntity.getSourceRef() == null) {
            codeListEntity.setSourceRef("");
        }
        if (codeListEntity.getDescription() == null) {
            codeListEntity.setDescription("");
        }
        if (codeListEntity.getColumns() == null) {
            codeListEntity.setColumns(List.of());
        }
        if (codeListEntity.getValueColumnIndex() == null) {
            codeListEntity.setValueColumnIndex(0);
        }
        if (codeListEntity.getLabelColumnIndex() == null) {
            codeListEntity.setLabelColumnIndex(0);
        }
        if (codeListEntity.getStatus() == null) {
            codeListEntity.setStatus(isSyncable(codeListEntity) ? CodeListStatus.SyncPending : CodeListStatus.Synced);
        }

        if (codeListEntity.getKey() == null || codeListEntity.getKey().isBlank()) {
            throw ResponseException.badRequest("Der Schlüssel der Codeliste darf nicht leer sein.");
        }

        if (codeListEntity.getKey().length() > 255) {
            throw ResponseException.badRequest("Der Schlüssel der Codeliste darf maximal 255 Zeichen lang sein.");
        }

        if ("new".equals(codeListEntity.getKey())) {
            throw ResponseException.badRequest("Der Schlüssel \"new\" ist für die Anlage neuer Codelisten reserviert.");
        }

        if (codeListEntity.getKey().contains("/")) {
            throw ResponseException.badRequest("Der Schlüssel der Codeliste darf keinen Schrägstrich enthalten.");
        }

        if (codeListEntity.getName() == null || codeListEntity.getName().isBlank()) {
            throw ResponseException.badRequest("Der Name der Codeliste darf nicht leer sein.");
        }

        if (isSyncable(codeListEntity) && codeListEntity.getSourceRef().isBlank()) {
            throw ResponseException.badRequest("Die Quelle der Codeliste darf nicht leer sein.");
        }

        if (codeListEntity.getSourceType() == CodeListSourceType.Manual && codeListEntity.getColumns().isEmpty()) {
            throw ResponseException.badRequest("Manuelle Codelisten benötigen mindestens eine Spalte.");
        }

        validateCodeListColumns(codeListEntity);
    }

    private void validateCodeListColumns(@Nonnull CodeListEntity codeListEntity) throws ResponseException {
        var columns = codeListEntity.getColumns();
        if (columns == null || columns.isEmpty()) {
            return;
        }

        if (codeListEntity.getValueColumnIndex() < 0 || codeListEntity.getValueColumnIndex() >= columns.size()) {
            throw ResponseException.badRequest("Der Index der Wert-Spalte ist ungültig.");
        }

        if (codeListEntity.getLabelColumnIndex() < 0 || codeListEntity.getLabelColumnIndex() >= columns.size()) {
            throw ResponseException.badRequest("Der Index der Beschriftungs-Spalte ist ungültig.");
        }
    }

    @Nonnull
    private List<String> normalizeItemColumns(@Nonnull CodeListEntity codeListEntity,
                                              @Nullable List<String> columns) throws ResponseException {
        if (columns == null || columns.size() != codeListEntity.getColumns().size()) {
            throw ResponseException.badRequest("Die Anzahl der Werte passt nicht zur Spaltenanzahl der Codeliste.");
        }

        return columns
                .stream()
                .map(value -> value == null ? "" : value)
                .toList();
    }

    @Nonnull
    private CodeListEntity requireCodeList(@Nonnull String codeListKey) throws ResponseException {
        return codeListRepository
                .findById(codeListKey)
                .orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    private CodeListEntity requireManualCodeList(@Nonnull String codeListKey) throws ResponseException {
        var codeList = requireCodeList(codeListKey);
        if (codeList.getSourceType() != CodeListSourceType.Manual) {
            throw ResponseException.methodNotAllowed("Nur manuelle Codelisten können direkt bearbeitet werden.");
        }
        return codeList;
    }

    @Nonnull
    private Integer requireInternalId(@Nonnull CodeListEntity codeList) throws ResponseException {
        var codeListId = codeList.getId();
        if (codeListId == null) {
            throw ResponseException.internalServerError("Die interne ID der Codeliste fehlt.");
        }
        return codeListId;
    }

    private boolean isSyncable(@Nonnull CodeListEntity codeListEntity) {
        return codeListEntity.getSourceType() == CodeListSourceType.XRepository ||
                codeListEntity.getSourceType() == CodeListSourceType.Asset;
    }

    private void markSyncing(@Nonnull CodeListEntity codeListEntity) {
        codeListEntity
                .setStatus(CodeListStatus.Syncing)
                .setStatusMessage(null);
        codeListRepository.save(codeListEntity);
    }

    private void markSynced(@Nonnull CodeListEntity codeListEntity) {
        codeListEntity
                .setStatus(CodeListStatus.Synced)
                .setStatusMessage(null)
                .setLastSync(Instant.now());
        codeListRepository.save(codeListEntity);
    }

    private void markSyncFailed(@Nonnull CodeListEntity codeListEntity, @Nullable String message) {
        codeListEntity
                .setStatus(CodeListStatus.SyncFailed)
                .setStatusMessage(message == null || message.isBlank() ? "Die Synchronisierung ist fehlgeschlagen." : message);
        codeListRepository.save(codeListEntity);
    }

    @Nonnull
    private String getColumnValue(@Nonnull CodeListItemEntity item, int columnIndex) {
        return item.getColumns().get(columnIndex);
    }
}
