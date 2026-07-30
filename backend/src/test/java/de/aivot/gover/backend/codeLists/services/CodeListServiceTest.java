package de.aivot.gover.backend.codeLists.services;

import de.aivot.gover.backend.asset.entities.AssetEntity;
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
import de.aivot.gover.backend.storage.services.StorageService;
import de.aivot.gover.backend.xrepository.models.*;
import de.aivot.gover.backend.xrepository.services.XRepositoryCodeListService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CodeListServiceTest {
    @Test
    void createClearsClientProvidedId() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);
        var codeList = new CodeListEntity()
                .setKey("test")
                .setId(0)
                .setSourceType(CodeListSourceType.Manual)
                .setSourceRef("")
                .setName("Test")
                .setDescription("")
                .setColumns(List.of("code", "name"))
                .setValueColumnIndex(0)
                .setLabelColumnIndex(1)
                .setStatus(CodeListStatus.Synced);

        when(codeListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(codeList);

        assertNull(created.getId());
    }

    @Test
    void createRejectsReservedKey() {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);
        var codeList = createManualCodeList().setKey("new");

        assertThrows(ResponseException.class, () -> service.create(codeList));

        verify(codeListRepository, never()).save(any());
    }

    @Test
    void createRejectsKeyWithSlash() {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);
        var codeList = createManualCodeList().setKey("a/b");

        assertThrows(ResponseException.class, () -> service.create(codeList));

        verify(codeListRepository, never()).save(any());
    }

    @Test
    void getXRepositoryColumnsReadsColumnRefs() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);

        when(xRepositoryCodeListService.getCodeList("urn:test")).thenReturn(createXRepositoryCodeList());

        assertEquals(List.of("code", "name"), service.getXRepositoryColumns("urn:test"));
    }

    @Test
    void getAssetColumnsReadsCsvHeader() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);
        var assetKey = UUID.randomUUID();
        var asset = new AssetEntity()
                .setKey(assetKey)
                .setPrivate(true)
                .setStorageProviderId(3)
                .setStoragePathFromRoot("/code-lists/test.csv");

        when(assetService.retrieve(assetKey)).thenReturn(Optional.of(asset));
        when(storageService.getDocumentContent(3, "/code-lists/test.csv"))
                .thenReturn(new ByteArrayInputStream("""
                        code,name
                        001,Berlin
                        """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(List.of("code", "name"), service.getAssetColumns(assetKey));
    }

    @Test
    void getAssetColumnsUsesFallbackForMissingCsvHeaderColumns() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);
        var assetKey = UUID.randomUUID();
        var asset = new AssetEntity()
                .setKey(assetKey)
                .setPrivate(true)
                .setStorageProviderId(3)
                .setStoragePathFromRoot("/code-lists/test.csv");

        when(assetService.retrieve(assetKey)).thenReturn(Optional.of(asset));
        when(storageService.getDocumentContent(3, "/code-lists/test.csv"))
                .thenReturn(new ByteArrayInputStream("""
                        code,,name,
                        001,x,Berlin,y
                        """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(List.of("code", "COL1", "name", "COL3"), service.getAssetColumns(assetKey));
    }

    @Test
    void syncXRepositoryReplacesItems() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);
        var codeList = createCodeList();

        when(codeListRepository.findById("test")).thenReturn(Optional.of(codeList));
        when(xRepositoryCodeListService.getCodeList("urn:test")).thenReturn(createXRepositoryCodeList(
                row(value("code", "001"), value("name", "Berlin")),
                row(value("code", "002"), value("name", "Hamburg"))
        ));

        service.syncCodeList("test", false);

        verify(codeListItemRepository).deleteAllByCodeListId(7);

        @SuppressWarnings({"rawtypes", "unchecked"})
        var itemsCaptor = (ArgumentCaptor<Iterable<CodeListItemEntity>>) (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
        verify(codeListItemRepository).saveAll(itemsCaptor.capture());
        var savedItems = StreamSupport
                .stream(itemsCaptor.getValue().spliterator(), false)
                .toList();

        assertEquals(CodeListStatus.Synced, codeList.getStatus());
        assertEquals(List.of("code", "name"), codeList.getColumns());
        assertEquals(List.of("001", "Berlin"), savedItems.getFirst().getColumns());
        assertEquals(List.of("002", "Hamburg"), savedItems.get(1).getColumns());
    }

    @Test
    void syncXRepositoryMarksDuplicateValuesFailed() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);
        var codeList = createCodeList();

        when(codeListRepository.findById("test")).thenReturn(Optional.of(codeList));
        when(xRepositoryCodeListService.getCodeList("urn:test")).thenReturn(createXRepositoryCodeList(
                row(value("code", "001"), value("name", "Berlin")),
                row(value("code", "001"), value("name", "Berlin duplicate"))
        ));

        service.syncCodeList("test", false);

        verify(codeListItemRepository, never()).saveAll(any());
        assertEquals(CodeListStatus.SyncFailed, codeList.getStatus());
        assertTrue(codeList.getStatusMessage().contains("mehrfach"));
    }

    @Test
    void syncAssetReplacesItemsFromCsv() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);
        var assetKey = UUID.randomUUID();
        var codeList = createAssetCodeList(assetKey);
        var asset = new AssetEntity()
                .setKey(assetKey)
                .setPrivate(true)
                .setStorageProviderId(3)
                .setStoragePathFromRoot("/code-lists/test.csv");

        when(codeListRepository.findById("test")).thenReturn(Optional.of(codeList));
        when(assetService.retrieve(assetKey)).thenReturn(Optional.of(asset));
        when(storageService.getDocumentContent(3, "/code-lists/test.csv"))
                .thenReturn(new ByteArrayInputStream("""
                        code,name
                        001,Berlin
                        002,Hamburg
                        """.getBytes(StandardCharsets.UTF_8)));

        service.syncCodeList("test", false);

        verify(codeListItemRepository).deleteAllByCodeListId(7);

        @SuppressWarnings({"rawtypes", "unchecked"})
        var itemsCaptor = (ArgumentCaptor<Iterable<CodeListItemEntity>>) (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
        verify(codeListItemRepository).saveAll(itemsCaptor.capture());
        var savedItems = StreamSupport
                .stream(itemsCaptor.getValue().spliterator(), false)
                .toList();

        assertEquals(CodeListStatus.Synced, codeList.getStatus());
        assertEquals(List.of("code", "name"), codeList.getColumns());
        assertEquals(List.of("001", "Berlin"), savedItems.getFirst().getColumns());
        assertEquals(List.of("002", "Hamburg"), savedItems.get(1).getColumns());
    }

    @Test
    void syncAssetUsesFallbackForMissingCsvHeaderColumns() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);
        var assetKey = UUID.randomUUID();
        var codeList = createAssetCodeList(assetKey);
        var asset = new AssetEntity()
                .setKey(assetKey)
                .setPrivate(true)
                .setStorageProviderId(3)
                .setStoragePathFromRoot("/code-lists/test.csv");

        when(codeListRepository.findById("test")).thenReturn(Optional.of(codeList));
        when(assetService.retrieve(assetKey)).thenReturn(Optional.of(asset));
        when(storageService.getDocumentContent(3, "/code-lists/test.csv"))
                .thenReturn(new ByteArrayInputStream("""
                        code,,name
                        001,x,Berlin
                        """.getBytes(StandardCharsets.UTF_8)));

        service.syncCodeList("test", false);

        assertEquals(CodeListStatus.Synced, codeList.getStatus());
        assertEquals(List.of("code", "COL1", "name"), codeList.getColumns());
    }

    @Test
    void exportCsvWritesColumnsAndItems() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);
        var codeList = createManualCodeList();

        when(codeListRepository.findById("test")).thenReturn(Optional.of(codeList));
        when(vCodeListItemRepository.findAllByCodeListIdOrderByIdAsc(7)).thenReturn(List.of(
                new VCodeListItemEntity()
                        .setId(1L)
                        .setCodeListId(7)
                        .setColumns(List.of("001", "Berlin"))
                        .setValue("001")
                        .setLabel("Berlin"),
                new VCodeListItemEntity()
                        .setId(2L)
                        .setCodeListId(7)
                        .setColumns(List.of("00,2", "Ham\"burg"))
                        .setValue("00,2")
                        .setLabel("Ham\"burg")
        ));

        var csv = new String(service.exportCSV("test"), StandardCharsets.UTF_8);

        assertEquals("code,name\r\n001,Berlin\r\n\"00,2\",\"Ham\"\"burg\"\r\n", csv);
    }

    @Test
    void importManualCsvReplacesItemsAndUpdatesColumns() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);
        var codeList = createManualCodeList()
                .setColumns(List.of("old"))
                .setValueColumnIndex(0)
                .setLabelColumnIndex(0);

        when(codeListRepository.findById("test")).thenReturn(Optional.of(codeList));
        when(codeListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var imported = service.importCSV(
                "test",
                new ByteArrayInputStream("""
                        code,name
                        001,Berlin
                        002,Hamburg
                        """.getBytes(StandardCharsets.UTF_8))
        );

        verify(codeListItemRepository).deleteAllByCodeListId(7);

        @SuppressWarnings({"rawtypes", "unchecked"})
        var itemsCaptor = (ArgumentCaptor<Iterable<CodeListItemEntity>>) (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
        verify(codeListItemRepository).saveAll(itemsCaptor.capture());
        var savedItems = StreamSupport
                .stream(itemsCaptor.getValue().spliterator(), false)
                .toList();

        assertEquals(List.of("code", "name"), imported.getColumns());
        assertEquals(0, imported.getValueColumnIndex());
        assertEquals(0, imported.getLabelColumnIndex());
        assertEquals(List.of("001", "Berlin"), savedItems.getFirst().getColumns());
        assertEquals(List.of("002", "Hamburg"), savedItems.get(1).getColumns());
    }

    @Test
    void importManualCsvRejectsDuplicateValuesBeforeDeletingItems() {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);
        var codeList = createManualCodeList();

        when(codeListRepository.findById("test")).thenReturn(Optional.of(codeList));

        assertThrows(ResponseException.class, () -> service.importCSV(
                "test",
                new ByteArrayInputStream("""
                        code,name
                        001,Berlin
                        001,Hamburg
                        """.getBytes(StandardCharsets.UTF_8))
        ));

        verify(codeListItemRepository, never()).deleteAllByCodeListId(any());
        verify(codeListItemRepository, never()).saveAll(any());
    }

    @Test
    void importCsvRejectsNonManualCodeLists() {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService);
        var codeList = createCodeList();

        when(codeListRepository.findById("test")).thenReturn(Optional.of(codeList));

        assertThrows(ResponseException.class, () -> service.importCSV(
                "test",
                new ByteArrayInputStream("""
                        code,name
                        001,Berlin
                        """.getBytes(StandardCharsets.UTF_8))
        ));

        verify(codeListItemRepository, never()).deleteAllByCodeListId(any());
        verify(codeListItemRepository, never()).saveAll(any());
    }

    private static CodeListEntity createManualCodeList() {
        return new CodeListEntity()
                .setKey("test")
                .setId(7)
                .setSourceType(CodeListSourceType.Manual)
                .setSourceRef("")
                .setName("Test")
                .setDescription("")
                .setColumns(List.of("code", "name"))
                .setValueColumnIndex(0)
                .setLabelColumnIndex(1)
                .setStatus(CodeListStatus.Synced);
    }

    private static CodeListEntity createCodeList() {
        return new CodeListEntity()
                .setKey("test")
                .setId(7)
                .setSourceType(CodeListSourceType.XRepository)
                .setSourceRef("urn:test")
                .setName("Test")
                .setDescription("")
                .setColumns(List.of())
                .setValueColumnIndex(0)
                .setLabelColumnIndex(0)
                .setStatus(CodeListStatus.SyncPending);
    }

    private static CodeListEntity createAssetCodeList(UUID assetKey) {
        return new CodeListEntity()
                .setKey("test")
                .setId(7)
                .setSourceType(CodeListSourceType.Asset)
                .setSourceRef(assetKey.toString())
                .setName("Test")
                .setDescription("")
                .setColumns(List.of())
                .setValueColumnIndex(0)
                .setLabelColumnIndex(0)
                .setStatus(CodeListStatus.SyncPending);
    }

    private static XRepositoryCodeList createXRepositoryCodeList(XRepositoryCodeListSimpleCodeListRow... rows) {
        return new XRepositoryCodeList()
                .setColumnSet(new XRepositoryCodeListColumnSet()
                        .setColumn(List.of(column("code"), column("name")))
                        .setKey(new XRepositoryCodeListColumnSetKey()
                                .setColumnRef(new XRepositoryCodeListColumnSetKeyRef().setRef("code"))))
                .setCodeList(new XRepositoryCodeListSimpleCodeList()
                        .setRow(List.of(rows)));
    }

    private static XRepositoryCodeListColumnSetColumn column(String id) {
        return new XRepositoryCodeListColumnSetColumn().setId(id);
    }

    private static XRepositoryCodeListSimpleCodeListRow row(XRepositoryCodeListSimpleCodeListRowValue... values) {
        return new XRepositoryCodeListSimpleCodeListRow().setValue(List.of(values));
    }

    private static XRepositoryCodeListSimpleCodeListRowValue value(String columnRef, String value) {
        return new XRepositoryCodeListSimpleCodeListRowValue()
                .setColumnRef(columnRef)
                .setSimpleValue(value);
    }
}
