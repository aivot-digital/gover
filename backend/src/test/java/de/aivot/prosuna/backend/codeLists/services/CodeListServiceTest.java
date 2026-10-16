package de.aivot.prosuna.backend.codeLists.services;

import de.aivot.prosuna.backend.asset.entities.AssetEntity;
import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import de.aivot.prosuna.backend.codeLists.entities.CodeListItemEntity;
import de.aivot.prosuna.backend.codeLists.entities.VCodeListItemEntity;
import de.aivot.prosuna.backend.codeLists.enums.CodeListSourceType;
import de.aivot.prosuna.backend.codeLists.enums.CodeListStatus;
import de.aivot.prosuna.backend.codeLists.repositories.CodeListItemRepository;
import de.aivot.prosuna.backend.codeLists.repositories.CodeListRepository;
import de.aivot.prosuna.backend.codeLists.repositories.VCodeListItemRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.xrepository.models.*;
import de.aivot.prosuna.backend.xrepository.services.XRepositoryCodeListService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CodeListServiceTest {
    private static final Instant STALE_BEFORE = Instant.parse("2030-05-01T11:00:00Z");

    @ParameterizedTest
    @MethodSource("invalidItemColumns")
    void saveItemRejectsMissingRequiredValues(boolean update, int labelColumnIndex, List<String> columns, String message) {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository,
                mock(XRepositoryCodeListService.class), mock(AssetService.class), mock(StorageService.class), transactionManager());
        var codeList = createManualCodeList()
                .setColumns(List.of("Zusatz", "Anzeigename", "Schlüssel"))
                .setLabelColumnIndex(labelColumnIndex)
                .setValueColumnIndex(2);
        var originalColumns = List.of("", "Berlin", "BE");
        var existingItem = new CodeListItemEntity().setId(10L).setCodeListId(7).setColumns(originalColumns);
        var item = new CodeListItemEntity().setColumns(columns);
        when(codeListRepository.findById("test")).thenReturn(Optional.of(codeList));
        if (update) {
            when(codeListItemRepository.findByIdAndCodeListId(10L, 7)).thenReturn(Optional.of(existingItem));
        }

        var exception = assertThrows(ResponseException.class, () -> {
            if (update) {
                service.updateItem("test", 10L, item);
            } else {
                service.createItem("test", item);
            }
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(message, exception.getMessage());
        assertEquals(originalColumns, existingItem.getColumns());
        verify(codeListItemRepository, never()).save(any());
        verifyNoInteractions(vCodeListItemRepository);
    }

    private static Stream<Arguments> invalidItemColumns() {
        return Stream.of(false, true).flatMap(update -> Stream.of(
                Arguments.of(update, 1, List.of("", "", "BE"), "Bitte geben Sie für „Anzeigename“ einen Wert ein."),
                Arguments.of(update, 1, Arrays.asList("", null, "BE"), "Bitte geben Sie für „Anzeigename“ einen Wert ein."),
                Arguments.of(update, 1, List.of("", " \t\n", "BE"), "Bitte geben Sie für „Anzeigename“ einen Wert ein."),
                Arguments.of(update, 1, List.of("", "Berlin", ""), "Bitte geben Sie für „Schlüssel“ einen Wert ein."),
                Arguments.of(update, 1, Arrays.asList("", "Berlin", null), "Bitte geben Sie für „Schlüssel“ einen Wert ein."),
                Arguments.of(update, 1, List.of("", "Berlin", " \t\n"), "Bitte geben Sie für „Schlüssel“ einen Wert ein."),
                Arguments.of(update, 2, List.of("", "", ""), "Bitte geben Sie für „Schlüssel“ einen Wert ein."),
                Arguments.of(update, 1, null, "Die Anzahl der Werte passt nicht zur Spaltenanzahl der Codeliste."),
                Arguments.of(update, 1, List.of("", "Berlin"), "Die Anzahl der Werte passt nicht zur Spaltenanzahl der Codeliste.")
        ));
    }

    @ParameterizedTest
    @CsvSource({"false, 1", "true, 1", "false, 2", "true, 2"})
    void saveItemAllowsEmptyOptionalColumnsAndSharedLabelValueColumn(boolean update, int labelColumnIndex) throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository,
                mock(XRepositoryCodeListService.class), mock(AssetService.class), mock(StorageService.class), transactionManager());
        var codeList = createManualCodeList()
                .setColumns(List.of("Zusatz", "Anzeigename", "Schlüssel"))
                .setLabelColumnIndex(labelColumnIndex)
                .setValueColumnIndex(2);
        var label = labelColumnIndex == 1 ? "Berlin" : "";
        var item = new CodeListItemEntity().setColumns(Arrays.asList(null, label, "BE"));
        var existingItem = new CodeListItemEntity().setId(10L).setCodeListId(7).setColumns(List.of("", "Alt", "ALT"));
        var expectedColumns = List.of("", label, "BE");
        var view = new VCodeListItemEntity().setId(10L).setCodeListId(7).setColumns(expectedColumns);
        when(codeListRepository.findById("test")).thenReturn(Optional.of(codeList));
        when(vCodeListItemRepository.findByIdAndCodeListId(10L, 7)).thenReturn(Optional.of(view));
        when(codeListItemRepository.save(any())).thenAnswer(invocation -> ((CodeListItemEntity) invocation.getArgument(0)).setId(10L));
        if (update) {
            when(codeListItemRepository.findByIdAndCodeListId(10L, 7)).thenReturn(Optional.of(existingItem));
        }

        var result = update ? service.updateItem("test", 10L, item) : service.createItem("test", item);

        assertEquals(view, result);
        var savedItem = ArgumentCaptor.forClass(CodeListItemEntity.class);
        verify(codeListItemRepository).save(savedItem.capture());
        assertEquals(expectedColumns, savedItem.getValue().getColumns());
        assertEquals(7, savedItem.getValue().getCodeListId());
    }

    @Test
    void createClearsClientProvidedId() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService, transactionManager());
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
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService, transactionManager());
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
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService, transactionManager());
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
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService, transactionManager());

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
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService, transactionManager());
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
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService, transactionManager());
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
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, mock(VCodeListItemRepository.class),
                xRepositoryCodeListService, mock(AssetService.class), mock(StorageService.class), transactionManager());
        var current = createCodeList();
        var claimedRun = claims(codeListRepository, current);

        when(codeListRepository.findById("test")).thenReturn(Optional.of(createCodeList()));
        when(xRepositoryCodeListService.getCodeList("urn:test")).thenReturn(createXRepositoryCodeList(
                row(value("code", "001"), value("name", "Berlin")),
                row(value("code", "002"), value("name", "Hamburg"))
        ));

        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListRepository).claimSync(eq("test"), any(), any(), eq(STALE_BEFORE));
        verify(codeListItemRepository).deleteAllByCodeListId(7);

        @SuppressWarnings({"rawtypes", "unchecked"})
        var itemsCaptor = (ArgumentCaptor<Iterable<CodeListItemEntity>>) (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
        verify(codeListItemRepository).saveAll(itemsCaptor.capture());
        var savedItems = StreamSupport
                .stream(itemsCaptor.getValue().spliterator(), false)
                .toList();

        verify(codeListRepository).save(current);
        assertEquals(CodeListStatus.Synced, current.getStatus());
        assertNull(current.getSyncRunId());
        assertNull(current.getSyncStarted());
        assertEquals(List.of("code", "name"), current.getColumns());
        assertEquals(List.of("001", "Berlin"), savedItems.getFirst().getColumns());
        assertEquals(List.of("002", "Hamburg"), savedItems.get(1).getColumns());
        verify(codeListRepository, never()).releaseSync(any(), any());
        assertEquals(1, claimedRun.size());
    }

    @Test
    void syncXRepositoryMarksDuplicateValuesFailed() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, mock(VCodeListItemRepository.class),
                xRepositoryCodeListService, mock(AssetService.class), mock(StorageService.class), transactionManager());
        var current = createCodeList();
        var claimedRun = claims(codeListRepository, current);

        when(codeListRepository.findById("test")).thenReturn(Optional.of(createCodeList()));
        when(xRepositoryCodeListService.getCodeList("urn:test")).thenReturn(createXRepositoryCodeList(
                row(value("code", "001"), value("name", "Berlin")),
                row(value("code", "001"), value("name", "Berlin duplicate"))
        ));
        when(codeListRepository.failSync(eq("test"), any(), any(), any())).thenReturn(1);

        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListItemRepository, never()).saveAll(any());
        verify(codeListRepository).failSync(eq("test"), eq(claimedRun.getFirst()), argThat(message -> message.contains("mehrfach")), any());
        verify(codeListRepository, never()).save(any());
        assertEquals(List.of(), current.getColumns());
    }

    @Test
    void syncAssetReplacesItemsFromCsv() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, mock(VCodeListItemRepository.class),
                mock(XRepositoryCodeListService.class), assetService, storageService, transactionManager());
        var assetKey = UUID.randomUUID();
        var current = createAssetCodeList(assetKey);
        var asset = new AssetEntity()
                .setKey(assetKey)
                .setPrivate(true)
                .setStorageProviderId(3)
                .setStoragePathFromRoot("/code-lists/test.csv");
        claims(codeListRepository, current);

        when(codeListRepository.findById("test")).thenReturn(Optional.of(createAssetCodeList(assetKey)));
        when(assetService.retrieve(assetKey)).thenReturn(Optional.of(asset));
        when(storageService.getDocumentContent(3, "/code-lists/test.csv"))
                .thenReturn(new ByteArrayInputStream("""
                        code,name
                        001,Berlin
                        002,Hamburg
                        """.getBytes(StandardCharsets.UTF_8)));

        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListItemRepository).deleteAllByCodeListId(7);

        @SuppressWarnings({"rawtypes", "unchecked"})
        var itemsCaptor = (ArgumentCaptor<Iterable<CodeListItemEntity>>) (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
        verify(codeListItemRepository).saveAll(itemsCaptor.capture());
        var savedItems = StreamSupport
                .stream(itemsCaptor.getValue().spliterator(), false)
                .toList();

        verify(codeListRepository).save(current);
        assertEquals(CodeListStatus.Synced, current.getStatus());
        assertEquals(List.of("code", "name"), current.getColumns());
        assertEquals(List.of("001", "Berlin"), savedItems.getFirst().getColumns());
        assertEquals(List.of("002", "Hamburg"), savedItems.get(1).getColumns());
    }

    @Test
    void syncAssetUsesFallbackForMissingCsvHeaderColumns() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, mock(CodeListItemRepository.class), mock(VCodeListItemRepository.class),
                mock(XRepositoryCodeListService.class), assetService, storageService, transactionManager());
        var assetKey = UUID.randomUUID();
        var current = createAssetCodeList(assetKey);
        var asset = new AssetEntity()
                .setKey(assetKey)
                .setPrivate(true)
                .setStorageProviderId(3)
                .setStoragePathFromRoot("/code-lists/test.csv");
        claims(codeListRepository, current);

        when(codeListRepository.findById("test")).thenReturn(Optional.of(createAssetCodeList(assetKey)));
        when(assetService.retrieve(assetKey)).thenReturn(Optional.of(asset));
        when(storageService.getDocumentContent(3, "/code-lists/test.csv"))
                .thenReturn(new ByteArrayInputStream("""
                        code,,name
                        001,x,Berlin
                        """.getBytes(StandardCharsets.UTF_8)));

        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListRepository).save(current);
        assertEquals(List.of("code", "COL1", "name"), current.getColumns());
    }

    @Test
    void syncKeepsOutdatedItemsWhenRequested() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, mock(VCodeListItemRepository.class),
                xRepositoryCodeListService, mock(AssetService.class), mock(StorageService.class), transactionManager());
        var current = createCodeList().setSyncKeepOutdated(true);
        claims(codeListRepository, current);

        when(codeListRepository.findById("test")).thenReturn(Optional.of(createCodeList().setSyncKeepOutdated(true)));
        when(xRepositoryCodeListService.getCodeList("urn:test")).thenReturn(createXRepositoryCodeList(
                row(value("code", "001"), value("name", "Berlin"))
        ));

        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListItemRepository, never()).deleteAllByCodeListId(any());
        verify(codeListItemRepository).saveAll(any());
        verify(codeListRepository).save(current);
    }

    @Test
    void syncThatIsNotPendingDoesNothing() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var service = new CodeListService(codeListRepository, mock(CodeListItemRepository.class), mock(VCodeListItemRepository.class),
                xRepositoryCodeListService, mock(AssetService.class), mock(StorageService.class), transactionManager());

        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListRepository, never()).findById(any());
        verify(codeListRepository, never()).save(any());
        verifyNoInteractions(xRepositoryCodeListService);
    }

    @Test
    void changeDuringTheSyncIsKeptAndRequestedAgain() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, mock(VCodeListItemRepository.class),
                xRepositoryCodeListService, mock(AssetService.class), mock(StorageService.class), transactionManager());
        var current = createCodeList();
        var claimedRun = claims(codeListRepository, current);

        when(codeListRepository.findById("test")).thenReturn(Optional.of(createCodeList()));
        when(xRepositoryCodeListService.getCodeList("urn:test")).thenAnswer(call -> {
            // Staff change the source while it is read, which requests another synchronization.
            current.setSourceRef("urn:changed").setStatus(CodeListStatus.SyncPending).setSyncKeepOutdated(true);
            return createXRepositoryCodeList(row(value("code", "001"), value("name", "Berlin")));
        });

        assertTrue(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListItemRepository, never()).deleteAllByCodeListId(any());
        verify(codeListItemRepository, never()).saveAll(any());
        verify(codeListRepository, never()).save(any());
        verify(codeListRepository, never()).failSync(any(), any(), any(), any());
        verify(codeListRepository).releaseSync("test", claimedRun.getFirst());
        assertEquals("urn:changed", current.getSourceRef());
        assertEquals(CodeListStatus.SyncPending, current.getStatus());
        assertTrue(current.getSyncKeepOutdated());
    }

    @Test
    void runThatWasTakenOverDiscardsItsResult() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, mock(VCodeListItemRepository.class),
                xRepositoryCodeListService, mock(AssetService.class), mock(StorageService.class), transactionManager());
        var current = createCodeList();
        claims(codeListRepository, current);
        var otherRun = UUID.randomUUID();

        when(codeListRepository.findById("test")).thenReturn(Optional.of(createCodeList()));
        when(xRepositoryCodeListService.getCodeList("urn:test")).thenAnswer(call -> {
            // The run took longer than the stale threshold, so another run took the synchronization over.
            current.setSyncRunId(otherRun);
            return createXRepositoryCodeList(row(value("code", "001"), value("name", "Berlin")));
        });

        service.syncCodeList("test", STALE_BEFORE);

        verify(codeListItemRepository, never()).saveAll(any());
        verify(codeListRepository, never()).save(any());
        assertEquals(otherRun, current.getSyncRunId());
        assertEquals(CodeListStatus.Syncing, current.getStatus());
    }

    @Test
    void failureOfASupersededRunIsNotRecorded() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var service = new CodeListService(codeListRepository, mock(CodeListItemRepository.class), mock(VCodeListItemRepository.class),
                xRepositoryCodeListService, mock(AssetService.class), mock(StorageService.class), transactionManager());
        var claimedRun = claims(codeListRepository, createCodeList());

        when(codeListRepository.findById("test")).thenReturn(Optional.of(createCodeList()));
        when(xRepositoryCodeListService.getCodeList("urn:test")).thenThrow(ResponseException.internalServerError("XRepository nicht erreichbar"));

        assertTrue(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListRepository).failSync(eq("test"), eq(claimedRun.getFirst()), any(), any());
        verify(codeListRepository).releaseSync("test", claimedRun.getFirst());
    }

    @Test
    void requestSyncStoresTheRequestWithTheCodeList() {
        var codeListRepository = mock(CodeListRepository.class);
        var service = new CodeListService(codeListRepository, mock(CodeListItemRepository.class), mock(VCodeListItemRepository.class),
                mock(XRepositoryCodeListService.class), mock(AssetService.class), mock(StorageService.class), transactionManager());
        when(codeListRepository.requestSync(eq("test"), eq(true), any())).thenReturn(1);

        assertTrue(service.requestSync("test", true));
        assertFalse(service.requestSync("manual", false));
    }


    @Test
    void exportCsvWritesColumnsAndItems() throws Exception {
        var codeListRepository = mock(CodeListRepository.class);
        var codeListItemRepository = mock(CodeListItemRepository.class);
        var vCodeListItemRepository = mock(VCodeListItemRepository.class);
        var xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        var assetService = mock(AssetService.class);
        var storageService = mock(StorageService.class);
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService, transactionManager());
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
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService, transactionManager());
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
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService, transactionManager());
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
        var service = new CodeListService(codeListRepository, codeListItemRepository, vCodeListItemRepository, xRepositoryCodeListService, assetService, storageService, transactionManager());
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

    /** Claims the synchronization of the given list as the locked current state. Returns the claimed runs. */
    private static List<UUID> claims(CodeListRepository codeListRepository, CodeListEntity current) {
        var runs = new ArrayList<UUID>();
        when(codeListRepository.claimSync(eq("test"), any(), any(), any())).thenAnswer(call -> {
            UUID runId = call.getArgument(1);
            runs.add(runId);
            current.setStatus(CodeListStatus.Syncing).setSyncRunId(runId).setSyncStarted(call.getArgument(2));
            return 1;
        });
        when(codeListRepository.findByKeyForUpdate("test")).thenReturn(Optional.of(current));
        return runs;
    }

    private static PlatformTransactionManager transactionManager() {
        var transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        return transactionManager;
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
                .setStatus(CodeListStatus.Syncing)
                .setSyncKeepOutdated(false);
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
                .setStatus(CodeListStatus.Syncing)
                .setSyncKeepOutdated(false);
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
