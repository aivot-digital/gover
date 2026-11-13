package de.aivot.prosuna.backend.codeLists.services;

import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.codeLists.dtos.CodeListSourceColumnsDto;
import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import de.aivot.prosuna.backend.codeLists.entities.CodeListItemEntity;
import de.aivot.prosuna.backend.codeLists.enums.CodeListSourceType;
import de.aivot.prosuna.backend.codeLists.enums.CodeListStatus;
import de.aivot.prosuna.backend.codeLists.models.CodeListColumnMapping;
import de.aivot.prosuna.backend.codeLists.repositories.CodeListItemRepository;
import de.aivot.prosuna.backend.codeLists.repositories.CodeListRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.utils.StringUtils;
import de.aivot.prosuna.backend.xrepository.models.XRepositoryCodeList;
import de.aivot.prosuna.backend.xrepository.services.XRepositoryCodeListService;
import de.aivot.prosuna.backend.lib.models.LockVersioned;
import de.siegmar.fastcsv.reader.CsvParseException;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

@Service
public class CodeListService implements EntityService<CodeListEntity, String> {
    private static final int MAX_COLUMN_KEY_LENGTH = 255;

    private final CodeListRepository codeListRepository;
    private final CodeListItemRepository codeListItemRepository;
    private final XRepositoryCodeListService xRepositoryCodeListService;
    private final AssetService assetService;
    private final StorageService storageService;
    private final TransactionTemplate transaction;
    private final MeterRegistry meterRegistry;


    @Autowired
    public CodeListService(
            CodeListRepository codeListRepository,
            CodeListItemRepository codeListItemRepository,
            XRepositoryCodeListService xRepositoryCodeListService,
            AssetService assetService,
            StorageService storageService,
            PlatformTransactionManager transactionManager,
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry = meterRegistry;
        this.codeListRepository = codeListRepository;
        this.codeListItemRepository = codeListItemRepository;
        this.xRepositoryCodeListService = xRepositoryCodeListService;
        this.assetService = assetService;
        this.storageService = storageService;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Nonnull
    @Override
    public CodeListEntity create(@Nonnull CodeListEntity entity) throws ResponseException {
        entity.setId(null);
        // A lock version from the request would make Spring Data look for a row that does not exist yet.
        entity.setLockVersion(null);
        entity.setCreated(Instant.now());
        entity.setUpdated(Instant.now());
        // A new list may bring the columns of its source, so the first synchronization keeps the proposed mappings.
        normalizeCodeList(entity);

        if (codeListRepository.existsById(entity.getKey())) {
            throw ResponseException
                    .conflict("Eine Codeliste mit dem Schlüssel %s existiert bereits.", StringUtils.quote(entity.getKey()));
        }
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

    // Holds the lock of the list, so entries saved meanwhile cannot bypass the check of a changed key column.
    // ResponseException is checked, so without the rollback rule a rejected update would still flush its changes.
    @Nonnull
    @Override
    @Transactional(rollbackFor = ResponseException.class)
    public CodeListEntity update(@Nonnull String key, @Nonnull CodeListEntity entity) throws ResponseException {
        var existingEntity = codeListRepository
                .findByKeyForUpdate(key)
                .orElseThrow(ResponseException::notFound);
        return performUpdate(key, entity, existingEntity);
    }

    @Nonnull
    @Override
    public CodeListEntity performUpdate(@Nonnull String key,
                                        @Nonnull CodeListEntity entity,
                                        @Nonnull CodeListEntity existingEntity) throws ResponseException {
        var previousKeyColumnIndex = findKeyColumnIndex(existingEntity);

        // The stored columns describe the existing item cells, so they are never taken from the client.
        existingEntity.setSourceType(entity.getSourceType());
        existingEntity.setSourceRef(entity.getSourceRef());
        existingEntity.setName(entity.getName());
        existingEntity.setDescription(entity.getDescription());
        existingEntity.setColumnMappings(entity.getColumnMappings());
        existingEntity.setKeyColumnKey(entity.getKeyColumnKey());
        normalizeCodeList(existingEntity);

        // Manual lists are not synchronized afterwards, so the stored rows must already fit a newly chosen key column.
        var keyColumn = existingEntity
                .findColumnMapping(existingEntity.getKeyColumnKey())
                .filter(mapping -> mapping.index() != null);
        if (existingEntity.getSourceType() == CodeListSourceType.Manual &&
                keyColumn.isPresent() &&
                !keyColumn.get().index().equals(previousKeyColumnIndex)) {
            var rows = codeListItemRepository
                    .findAllByCodeListIdOrderByIdAsc(requireInternalId(existingEntity))
                    .stream()
                    .map(CodeListItemEntity::getColumns)
                    .toList();
            validateKeys(rows, keyColumn.get());
        }

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

    /**
     * Requests a synchronization of a list with a synchronized source. The request is stored with the list, so it
     * survives a lost broker message. Returns whether a request was recorded.
     */
    public boolean requestSync(@Nonnull String codeListKey, boolean keepOutdated) {
        return codeListRepository.requestSync(codeListKey, keepOutdated, Instant.now()) == 1;
    }

    /**
     * Runs a requested synchronization. Only one run can claim a request, so duplicate messages do nothing. The
     * source is read without holding locks; the result is recorded under a lock and only while this run still owns
     * the synchronization. If the list was changed or requested again in the meantime, or a run that started after
     * {@code staleBefore} took it over, the result is discarded. Returns whether the list must be published again,
     * because a newer request is pending.
     */
    public boolean syncCodeList(@Nonnull String codeListKey, @Nonnull Instant staleBefore) {
        var runId = UUID.randomUUID();
        if (codeListRepository.claimSync(codeListKey, runId, Instant.now(), staleBefore) != 1) {
            return false;
        }

        try {
            var codeList = codeListRepository
                    .findById(codeListKey)
                    .orElse(null);

            if (codeList == null) {
                return false;
            }

            var source = switch (codeList.getSourceType()) {
                case XRepository -> readXRepositorySource(codeList.getSourceRef());
                case Asset -> readAssetSource(parseAssetKey(codeList.getSourceRef()));
                case Manual, Plugin -> null;
            };

            var recorded = transaction.execute(status -> recordSync(codeListKey, runId, source));
            if (Boolean.TRUE.equals(recorded)) {
                meterRegistry.counter("prosuna.sync.runs", "type", "code_list", "outcome", "success").increment();
                return false;
            }
        } catch (Exception e) {
            var message = e.getMessage();
            var failed = codeListRepository.failSync(
                    codeListKey,
                    runId,
                    message == null || message.isBlank() ? "Die Synchronisierung ist fehlgeschlagen." : message,
                    Instant.now()
            );
            if (failed == 1) {
                meterRegistry.counter("prosuna.sync.runs", "type", "code_list", "outcome", "failed").increment();
                return false;
            }
        }

        // A newer request superseded this run. Releasing it lets the pending request be claimed.
        codeListRepository.releaseSync(codeListKey, runId);
        return true;
    }

    @Nonnull
    public List<String> findPendingSyncKeys(@Nonnull Instant requestedBefore) {
        return codeListRepository.findPendingSyncKeys(requestedBefore);
    }

    /** Records that a pending request is published again. Returns whether this caller may publish it. */
    public boolean claimSyncRedispatch(@Nonnull String codeListKey, @Nonnull Instant requestedBefore, @Nonnull Instant now) {
        return codeListRepository.claimSyncRedispatch(codeListKey, requestedBefore, now) == 1;
    }

    /** Returns synchronizations whose run started before {@code startedBefore} and is considered interrupted. */
    @Nonnull
    public List<String> findInterruptedSyncKeys(@Nonnull Instant startedBefore) {
        return codeListRepository.findInterruptedSyncKeys(startedBefore);
    }

    @Nonnull
    public byte[] exportCSV(@Nonnull String codeListKey) throws ResponseException {
        var codeList = requireCodeList(codeListKey);
        var items = listAllItems(codeList);
        var outputStream = new ByteArrayOutputStream();

        try (var csv = CsvWriter.builder().build(outputStream, StandardCharsets.UTF_8)) {
            csv.writeRecord(codeList.getColumns());
            for (var item : items) {
                // Rows of manual lists may lack the cells of columns added later; every record needs all fields.
                csv.writeRecord(IntStream
                        .rangeClosed(1, codeList.getColumns().size())
                        .mapToObj(item::cellAt)
                        .toList());
            }
        } catch (IOException | UncheckedIOException e) {
            throw ResponseException.internalServerError(e, "Die CSV-Datei konnte nicht erzeugt werden: %s", e.getMessage());
        }

        return outputStream.toByteArray();
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public CodeListEntity importCSV(@Nonnull String codeListKey,
                                    @Nonnull InputStream inputStream) throws ResponseException {
        var codeList = lockManualCodeList(codeListKey);
        applySource(codeList, readCsvSource(inputStream), false);
        return codeListRepository.save(codeList);
    }

    @Nonnull
    public CodeListSourceColumnsDto getXRepositoryColumns(@Nonnull String urn) throws ResponseException {
        return proposeColumns(readXRepositorySource(urn));
    }

    @Nonnull
    public CodeListSourceColumnsDto getAssetColumns(@Nonnull UUID assetKey) throws ResponseException {
        return proposeColumns(readAssetSource(assetKey));
    }

    @Nonnull
    public Page<CodeListItemEntity> listItems(@Nonnull String codeListKey, @Nonnull Pageable pageable) throws ResponseException {
        var codeListId = requireInternalId(requireCodeList(codeListKey));
        return codeListItemRepository.findAllByCodeListId(codeListId, pageable);
    }

    /**
     * Lists all rows with their cells in the column order of {@code codeList}, so its mappings read the right cells
     * even if a synchronization reordered the columns after the list was read.
     */
    @Nonnull
    public List<CodeListItemEntity> listAllItems(@Nonnull CodeListEntity codeList) throws ResponseException {
        return codeListItemRepository
                .findSnapshotsByCodeListId(requireInternalId(codeList))
                .stream()
                .map(row -> row.toItem(codeList.getColumns()))
                .toList();
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public CodeListItemEntity createItem(@Nonnull String codeListKey, @Nonnull CodeListItemEntity item) throws ResponseException {
        var codeList = lockManualCodeList(codeListKey);
        var codeListId = requireInternalId(codeList);
        item
                .setId(null)
                .setCodeListId(codeListId)
                .setColumns(normalizeItemColumns(codeList, null, item.getColumns()));

        return codeListItemRepository.save(item);
    }

    @Nonnull
    public CodeListItemEntity getItem(@Nonnull String codeListKey, @Nonnull Long itemId) throws ResponseException {
        var codeListId = requireInternalId(requireCodeList(codeListKey));
        return codeListItemRepository
                .findByIdAndCodeListId(itemId, codeListId)
                .orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public CodeListItemEntity updateItem(@Nonnull String codeListKey,
                                         @Nonnull Long itemId,
                                         @Nonnull CodeListItemEntity item) throws ResponseException {
        var codeList = lockManualCodeList(codeListKey);
        var codeListId = requireInternalId(codeList);
        var existingItem = codeListItemRepository
                .findByIdAndCodeListId(itemId, codeListId)
                .orElseThrow(ResponseException::notFound);
        LockVersioned.requireCurrent(item, existingItem);

        existingItem.setColumns(normalizeItemColumns(codeList, existingItem.getId(), item.getColumns()));
        return codeListItemRepository.save(existingItem);
    }

    public void deleteItem(@Nonnull String codeListKey, @Nonnull Long itemId) throws ResponseException {
        var codeList = requireManualCodeList(codeListKey);
        var codeListId = requireInternalId(codeList);
        var existingItem = codeListItemRepository
                .findByIdAndCodeListId(itemId, codeListId)
                .orElseThrow(ResponseException::notFound);
        codeListItemRepository.delete(existingItem);
    }

    /**
     * Matches the columns of a source with the existing mappings, so that keys and names stay stable when the source
     * reorders its columns. A mapping follows the source column that its index pointed to; if that column is gone,
     * the mapping keeps no index and remembers the column. Source columns without a mapping receive a new one, keyed
     * by the trimmed column name.
     */
    @Nonnull
    static List<CodeListColumnMapping> mergeColumnMappings(@Nonnull List<String> previousColumns,
                                                           @Nonnull List<CodeListColumnMapping> previousMappings,
                                                           @Nonnull List<String> columns,
                                                           @Nonnull Map<String, String> names) {
        var mappings = new ArrayList<CodeListColumnMapping>();
        var mappedColumns = new HashSet<String>();
        for (var mapping : previousMappings) {
            if (mapping.index() == null) {
                mappings.add(mapping);
                continue;
            }

            var column = columnAt(previousColumns, mapping.index());
            var position = column == null ? -1 : columns.indexOf(column);
            if (position < 0) {
                mappings.add(mapping.unbind(column));
            } else {
                mappedColumns.add(column);
                mappings.add(mapping.bindTo(position + 1));
            }
        }

        for (var i = 0; i < columns.size(); i++) {
            var column = columns.get(i);
            if (mappedColumns.contains(column)) {
                continue;
            }

            var index = i + 1;
            // A column that returns to the source gets its former mapping back, even if its key was renamed.
            var formerMapping = mappings
                    .stream()
                    .filter(mapping -> mapping.index() == null && column.equals(mapping.formerSourceColumn()))
                    .findFirst();
            if (formerMapping.isPresent()) {
                mappings.set(mappings.indexOf(formerMapping.get()), formerMapping.get().bindTo(index));
                continue;
            }

            // References trim column keys, so a key with surrounding whitespace could never be found.
            var columnKey = column.trim();
            var key = columnKey;
            for (var suffix = 2; containsKey(mappings, key); suffix++) {
                key = columnKey + "_" + suffix;
            }
            mappings.add(new CodeListColumnMapping(key, names.getOrDefault(column, columnKey).trim(), index));
        }

        return mappings;
    }

    /**
     * Records the result of a synchronization run in one transaction. Returns false without changes if the run no
     * longer owns the synchronization.
     */
    private boolean recordSync(@Nonnull String codeListKey,
                               @Nonnull UUID runId,
                               @Nullable SourceData source) {
        var codeList = codeListRepository
                .findByKeyForUpdate(codeListKey)
                .orElse(null);

        if (codeList == null || codeList.getStatus() != CodeListStatus.Syncing || !runId.equals(codeList.getSyncRunId())) {
            return false;
        }

        try {
            if (source != null) {
                applySource(codeList, source, codeList.getSyncKeepOutdated());
            }
        } catch (ResponseException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        codeListRepository.save(codeList
                .setStatus(CodeListStatus.Synced)
                .setStatusMessage(null)
                .setLastSync(Instant.now())
                .setSyncRunId(null)
                .setSyncStarted(null));
        return true;
    }

    /**
     * Replaces the columns and rows of the list with those of the source. With {@code keepOutdated}, rows that are
     * missing in the source are kept; their cells are moved into the new column order, so the mappings still read
     * the right cells.
     */
    private void applySource(@Nonnull CodeListEntity codeList,
                             @Nonnull SourceData source,
                             boolean keepOutdated) throws ResponseException {
        var codeListId = requireInternalId(codeList);
        var mappings = mergeColumnMappings(codeList.getColumns(), codeList.getColumnMappings(), source.columns(), source.names());
        var keyColumn = resolveKeyColumn(codeList.getKeyColumnKey(), mappings, source);
        validateKeys(source.rows(), keyColumn);

        var importedItems = source
                .rows()
                .stream()
                .map(row -> new CodeListItemEntity()
                        .setCodeListId(codeListId)
                        .setColumns(row))
                .toList();

        if (keepOutdated) {
            var existingByKey = new HashMap<String, CodeListItemEntity>();
            var itemsToSave = new ArrayList<CodeListItemEntity>();
            for (var existingItem : codeListItemRepository.findAllByCodeListId(codeListId)) {
                existingItem.setColumns(existingItem.cellsInColumnOrder(codeList.getColumns(), source.columns()));
                existingByKey.putIfAbsent(existingItem.cellAt(keyColumn.index()), existingItem);
                itemsToSave.add(existingItem);
            }

            for (var importedItem : importedItems) {
                var existingItem = existingByKey.get(importedItem.cellAt(keyColumn.index()));
                if (existingItem == null) {
                    itemsToSave.add(importedItem);
                } else {
                    existingItem.setColumns(importedItem.getColumns());
                }
            }

            // Kept rows have no value in a key column that is new in the source, and older rows may repeat a key.
            var keptRows = itemsToSave.stream().map(CodeListItemEntity::getColumns).toList();
            if (!identifiesRows(keptRows, keyColumn.index())) {
                throw ResponseException.conflict(
                        "Veraltete Einträge können nicht beibehalten werden, weil sie in der Schlüsselspalte %s leere oder doppelte Werte haben. Synchronisieren Sie die Codeliste ohne veraltete Einträge.",
                        StringUtils.quote(keyColumn.name())
                );
            }

            codeListItemRepository.saveAll(itemsToSave);
        } else {
            codeListItemRepository.deleteAllByCodeListId(codeListId);
            codeListItemRepository.saveAll(importedItems);
        }

        codeList
                .setColumns(source.columns())
                .setColumnMappings(mappings)
                .setKeyColumnKey(keyColumn.key());
    }

    /**
     * Keeps the configured key column while the source still contains it. Otherwise, e.g. for a new list, the key
     * column of an XRepository list or the first column whose values identify every row is used.
     */
    @Nonnull
    private static CodeListColumnMapping resolveKeyColumn(@Nullable String keyColumnKey,
                                                          @Nonnull List<CodeListColumnMapping> mappings,
                                                          @Nonnull SourceData source) {
        var configuredKeyColumn = mappings
                .stream()
                .filter(mapping -> mapping.key().equals(keyColumnKey) && mapping.index() != null)
                .findFirst();
        if (configuredKeyColumn.isPresent()) {
            return configuredKeyColumn.get();
        }

        var keyColumnIndex = findDefaultKeyColumnIndex(source);
        return mappings
                .stream()
                .filter(mapping -> Objects.equals(mapping.index(), keyColumnIndex))
                .findFirst()
                .orElseThrow();
    }

    private static int findDefaultKeyColumnIndex(@Nonnull SourceData source) {
        var sourceKeyColumnPosition = source.keyColumnRef() == null ? -1 : source.columns().indexOf(source.keyColumnRef());
        if (sourceKeyColumnPosition >= 0) {
            return sourceKeyColumnPosition + 1;
        }

        for (var index = 1; index <= source.columns().size(); index++) {
            if (identifiesRows(source.rows(), index)) {
                return index;
            }
        }

        return 1;
    }

    /** Returns whether every row has a value in the column at {@code index} that no other row has. */
    private static boolean identifiesRows(@Nonnull List<List<String>> rows, int index) {
        var values = new HashSet<String>();
        for (var row : rows) {
            var value = cellAt(row, index);
            if (value.isBlank() || !values.add(value)) {
                return false;
            }
        }
        return true;
    }

    private static void validateKeys(@Nonnull List<List<String>> rows,
                                     @Nonnull CodeListColumnMapping keyColumn) throws ResponseException {
        var values = new HashSet<String>();
        for (var row : rows) {
            var value = cellAt(row, keyColumn.index());
            if (value.isBlank()) {
                throw ResponseException.conflict(
                        "Die Schlüsselspalte %s enthält leere Werte.",
                        StringUtils.quote(keyColumn.name())
                );
            }
            if (!values.add(value)) {
                throw ResponseException.conflict(
                        "Die Schlüsselspalte %s enthält den Wert %s mehrfach.",
                        StringUtils.quote(keyColumn.name()),
                        StringUtils.quote(value)
                );
            }
        }
    }

    @Nonnull
    private CodeListSourceColumnsDto proposeColumns(@Nonnull SourceData source) {
        var mappings = mergeColumnMappings(List.of(), List.of(), source.columns(), source.names());
        return new CodeListSourceColumnsDto(source.columns(), mappings, resolveKeyColumn(null, mappings, source).key());
    }

    @Nonnull
    private SourceData readXRepositorySource(@Nonnull String urn) throws ResponseException {
        var xRepositoryCodeList = xRepositoryCodeListService.getCodeList(urn);
        var columns = extractColumnRefs(xRepositoryCodeList);

        var names = new HashMap<String, String>();
        for (var column : xRepositoryCodeList.getColumnSet().getColumn()) {
            var name = StringUtils.isNotNullOrEmpty(column.getLongName()) ? column.getLongName() : column.getShortName();
            if (StringUtils.isNotNullOrEmpty(name)) {
                names.put(column.getId(), name.trim());
            }
        }

        var rows = new ArrayList<List<String>>();
        if (xRepositoryCodeList.getCodeList() != null && xRepositoryCodeList.getCodeList().getRow() != null) {
            for (var row : xRepositoryCodeList.getCodeList().getRow()) {
                var valuesByColumnRef = new HashMap<String, String>();
                if (row.getValue() != null) {
                    for (var value : row.getValue()) {
                        valuesByColumnRef.put(value.getColumnRef(), value.getSimpleValue() == null ? "" : value.getSimpleValue());
                    }
                }

                rows.add(columns
                        .stream()
                        .map(columnRef -> valuesByColumnRef.getOrDefault(columnRef, ""))
                        .toList());
            }
        }

        return new SourceData(columns, names, rows, extractKeyColumnRef(xRepositoryCodeList));
    }

    @Nonnull
    private SourceData readAssetSource(@Nonnull UUID assetKey) throws ResponseException {
        var asset = assetService
                .retrieve(assetKey)
                .orElseThrow(ResponseException::notFound);

        try (var inputStream = storageService.getDocumentContent(asset.getStorageProviderId(), asset.getStoragePathFromRoot())) {
            return readCsvSource(inputStream);
        } catch (IOException e) {
            throw ResponseException.internalServerError(e, "Die CSV-Datei konnte nicht geschlossen werden: %s", e.getMessage());
        }
    }

    @Nonnull
    private SourceData readCsvSource(@Nonnull InputStream inputStream) throws ResponseException {
        try (var csv = CsvReader.builder()
                .detectBomHeader(true)
                .ofCsvRecord(inputStream)
        ) {
            var iterator = csv.iterator();
            var columns = extractCsvHeader(iterator);
            var rows = new ArrayList<List<String>>();
            while (iterator.hasNext()) {
                var row = List.copyOf(iterator.next().getFields());
                if (row.size() != columns.size()) {
                    throw ResponseException.badRequest("Die CSV-Datei enthält Zeilen mit unterschiedlicher Spaltenanzahl.");
                }
                rows.add(row);
            }
            return new SourceData(columns, Map.of(), rows, null);
        } catch (CsvParseException | UncheckedIOException e) {
            throw ResponseException.badRequest("Die CSV-Datei konnte nicht gelesen werden: " + e.getMessage(), e);
        } catch (IOException e) {
            throw ResponseException.internalServerError(e, "Die CSV-Datei konnte nicht geschlossen werden: %s", e.getMessage());
        }
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

    @Nonnull
    private UUID parseAssetKey(@Nonnull String sourceRef) throws ResponseException {
        try {
            return UUID.fromString(sourceRef);
        } catch (IllegalArgumentException e) {
            throw ResponseException.badRequest("Die Dateireferenz der Codeliste ist keine gültige UUID.", e);
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
        if (codeListEntity.getColumnMappings() == null) {
            codeListEntity.setColumnMappings(List.of());
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

        normalizeColumnMappings(codeListEntity);
    }

    private void normalizeColumnMappings(@Nonnull CodeListEntity codeListEntity) throws ResponseException {
        var isManual = codeListEntity.getSourceType() == CodeListSourceType.Manual;
        var columns = codeListEntity.getColumns();
        // Manual lists have no source, so new columns extend the stored cells.
        var maxIndex = isManual ? columns.size() + codeListEntity.getColumnMappings().size() : columns.size();

        var mappings = new ArrayList<CodeListColumnMapping>();
        var keys = new HashSet<String>();
        var indexes = new HashSet<Integer>();
        for (var mapping : codeListEntity.getColumnMappings()) {
            if (mapping == null) {
                throw ResponseException.badRequest("Die Spalten der Codeliste sind ungültig.");
            }

            var key = mapping.key() == null ? "" : mapping.key().trim();
            var name = mapping.name() == null ? "" : mapping.name().trim();
            var index = mapping.index();
            if (key.isEmpty()) {
                throw ResponseException.badRequest("Der Schlüssel einer Spalte darf nicht leer sein.");
            }
            if (key.length() > MAX_COLUMN_KEY_LENGTH) {
                throw ResponseException.badRequest("Der Schlüssel der Spalte %s darf maximal %d Zeichen lang sein.", StringUtils.quote(key), MAX_COLUMN_KEY_LENGTH);
            }
            if (!keys.add(key)) {
                throw ResponseException.badRequest("Der Spaltenschlüssel %s ist mehrfach vergeben.", StringUtils.quote(key));
            }
            if (name.isEmpty()) {
                throw ResponseException.badRequest("Der Name der Spalte %s darf nicht leer sein.", StringUtils.quote(key));
            }
            if (index != null && (index < 1 || index > maxIndex)) {
                throw ResponseException.badRequest("Die Position der Spalte %s ist ungültig.", StringUtils.quote(key));
            }
            if (index != null && !indexes.add(index)) {
                throw ResponseException.badRequest("Die Position %d ist mehreren Spalten zugeordnet.", index);
            }
            // A mapping without an index waits for its former source column, also in manual lists that receive CSV imports.
            var formerSourceColumn = index == null ? mapping.formerSourceColumn() : null;
            mappings.add(new CodeListColumnMapping(key, name, index, formerSourceColumn));
        }

        if (isManual) {
            if (mappings.isEmpty()) {
                throw ResponseException.badRequest("Manuelle Codelisten benötigen mindestens eine Spalte.");
            }

            // A bound column keeps its source identifier, e.g. an untrimmed CSV header, since the next import matches
            // columns through it. Only a column without one is identified by its key.
            var size = Math.max(columns.size(), indexes.stream().max(Integer::compareTo).orElse(0));
            var manualColumns = new ArrayList<>(Collections.nCopies(size, ""));
            for (var mapping : mappings) {
                if (mapping.index() != null && mapping.index() <= columns.size()) {
                    manualColumns.set(mapping.index() - 1, columns.get(mapping.index() - 1));
                }
            }
            for (var mapping : mappings) {
                if (mapping.index() != null && manualColumns.get(mapping.index() - 1).isEmpty()) {
                    // The header of an exported CSV must stay unique, so it can be imported again.
                    var column = mapping.key();
                    for (var suffix = 2; manualColumns.contains(column); suffix++) {
                        column = mapping.key() + "_" + suffix;
                    }
                    manualColumns.set(mapping.index() - 1, column);
                }
            }
            codeListEntity.setColumns(manualColumns);
        }

        var keyColumnKey = StringUtils.toNullableTrimmedString(codeListEntity.getKeyColumnKey());
        if (keyColumnKey == null) {
            // A synchronized list without a key column receives a default one during its next synchronization.
            if (isManual) {
                throw ResponseException.badRequest("Bitte wählen Sie eine Schlüsselspalte aus.");
            }
        } else if (mappings.stream().noneMatch(mapping -> mapping.key().equals(keyColumnKey) && mapping.index() != null)) {
            throw ResponseException.badRequest("Die Schlüsselspalte muss eine vorhandene Spalte der Codeliste sein.");
        }

        codeListEntity
                .setColumnMappings(mappings)
                .setKeyColumnKey(keyColumnKey);
    }

    /** Pads the cells of a row and checks its key value. {@code itemId} is the ID of an updated row. */
    @Nonnull
    private List<String> normalizeItemColumns(@Nonnull CodeListEntity codeListEntity,
                                              @Nullable Long itemId,
                                              @Nullable List<String> columns) throws ResponseException {
        var size = codeListEntity.getColumns().size();
        if (columns == null || columns.size() > size) {
            throw ResponseException.badRequest("Die Anzahl der Werte passt nicht zur Spaltenanzahl der Codeliste.");
        }

        var normalizedColumns = IntStream
                .rangeClosed(1, size)
                .mapToObj(index -> cellAt(columns, index))
                .toList();

        var keyColumn = codeListEntity
                .findColumnMapping(codeListEntity.getKeyColumnKey())
                .filter(mapping -> mapping.index() != null)
                .orElseThrow(() -> ResponseException.badRequest("Die Codeliste hat keine gültige Schlüsselspalte."));
        var keyValue = normalizedColumns.get(keyColumn.index() - 1);
        if (keyValue.isBlank()) {
            throw ResponseException.badRequest("Bitte geben Sie für „%s“ einen Wert ein.", keyColumn.name());
        }
        // Item IDs start at 1, so 0 excludes no row when a new item is created.
        if (codeListItemRepository.existsByKeyValue(requireInternalId(codeListEntity), keyColumn.index(), keyValue, itemId == null ? 0 : itemId)) {
            throw ResponseException.conflict(
                    "Der Wert %s ist in der Schlüsselspalte %s bereits vergeben.",
                    StringUtils.quote(keyValue),
                    StringUtils.quote(keyColumn.name())
            );
        }

        return normalizedColumns;
    }

    @Nullable
    private static Integer findKeyColumnIndex(@Nonnull CodeListEntity codeListEntity) {
        return codeListEntity
                .findColumnMapping(codeListEntity.getKeyColumnKey())
                .map(CodeListColumnMapping::index)
                .orElse(null);
    }

    @Nonnull
    private CodeListEntity requireCodeList(@Nonnull String codeListKey) throws ResponseException {
        return codeListRepository
                .findById(codeListKey)
                .orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    private CodeListEntity requireManualCodeList(@Nonnull String codeListKey) throws ResponseException {
        return requireManual(requireCodeList(codeListKey));
    }

    /**
     * Locks a manual list until the transaction ends. The database cannot enforce unique keys, because the key column
     * differs per list, so every write that checks or changes keys serializes on this lock, like synchronizations do.
     */
    @Nonnull
    private CodeListEntity lockManualCodeList(@Nonnull String codeListKey) throws ResponseException {
        return requireManual(codeListRepository
                .findByKeyForUpdate(codeListKey)
                .orElseThrow(ResponseException::notFound));
    }

    @Nonnull
    private static CodeListEntity requireManual(@Nonnull CodeListEntity codeList) throws ResponseException {
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

    @Nullable
    private static String columnAt(@Nonnull List<String> columns, @Nullable Integer index) {
        return index == null || index < 1 || index > columns.size() ? null : columns.get(index - 1);
    }

    @Nonnull
    private static String cellAt(@Nonnull List<String> cells, int index) {
        var cell = index < 1 || index > cells.size() ? null : cells.get(index - 1);
        return cell == null ? "" : cell;
    }

    private static boolean containsKey(@Nonnull List<CodeListColumnMapping> mappings, @Nonnull String key) {
        return mappings.stream().anyMatch(mapping -> mapping.key().equals(key));
    }

    /** The columns and rows of a source. Names hold display names that differ from the column identifiers. */
    private record SourceData(
            @Nonnull List<String> columns,
            @Nonnull Map<String, String> names,
            @Nonnull List<List<String>> rows,
            @Nullable String keyColumnRef
    ) {
    }
}
