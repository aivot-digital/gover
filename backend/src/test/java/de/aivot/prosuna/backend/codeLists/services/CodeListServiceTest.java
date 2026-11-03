package de.aivot.prosuna.backend.codeLists.services;

import de.aivot.prosuna.backend.asset.entities.AssetEntity;
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
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.xrepository.models.*;
import de.aivot.prosuna.backend.xrepository.services.XRepositoryCodeListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.Map;
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
    private static final UUID ASSET_KEY = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private CodeListRepository codeListRepository;
    private CodeListItemRepository codeListItemRepository;
    private XRepositoryCodeListService xRepositoryCodeListService;
    private AssetService assetService;
    private StorageService storageService;
    private CodeListService service;

    @BeforeEach
    void setUp() {
        codeListRepository = mock(CodeListRepository.class);
        codeListItemRepository = mock(CodeListItemRepository.class);
        xRepositoryCodeListService = mock(XRepositoryCodeListService.class);
        assetService = mock(AssetService.class);
        storageService = mock(StorageService.class);
        service = new CodeListService(codeListRepository, codeListItemRepository, xRepositoryCodeListService,
                assetService, storageService, transactionManager());
    }

    // region Items of manual lists

    @ParameterizedTest
    @MethodSource("invalidItemColumns")
    void saveItemRejectsMissingKeyValueOrTooManyCells(boolean update, List<String> columns, String message) {
        var existingItem = new CodeListItemEntity().setId(10L).setCodeListId(7).setColumns(List.of("Berlin", "BE", ""));
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createStateList()));
        if (update) {
            when(codeListItemRepository.findByIdAndCodeListId(10L, 7)).thenReturn(Optional.of(existingItem));
        }

        var exception = assertThrows(ResponseException.class, () -> {
            var item = new CodeListItemEntity().setColumns(columns);
            if (update) {
                service.updateItem("test", 10L, item);
            } else {
                service.createItem("test", item);
            }
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(message, exception.getMessage());
        assertEquals(List.of("Berlin", "BE", ""), existingItem.getColumns());
        verify(codeListItemRepository, never()).save(any());
    }

    private static Stream<Arguments> invalidItemColumns() {
        return Stream.of(false, true).flatMap(update -> Stream.of(
                Arguments.of(update, List.of("Berlin", "", ""), "Bitte geben Sie für „Schlüssel“ einen Wert ein."),
                Arguments.of(update, Arrays.asList("Berlin", null, ""), "Bitte geben Sie für „Schlüssel“ einen Wert ein."),
                Arguments.of(update, List.of("Berlin", " \t\n", ""), "Bitte geben Sie für „Schlüssel“ einen Wert ein."),
                Arguments.of(update, List.of("Berlin"), "Bitte geben Sie für „Schlüssel“ einen Wert ein."),
                Arguments.of(update, null, "Die Anzahl der Werte passt nicht zur Spaltenanzahl der Codeliste."),
                Arguments.of(update, List.of("Berlin", "BE", "", "zu viel"), "Die Anzahl der Werte passt nicht zur Spaltenanzahl der Codeliste.")
        ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void saveItemAllowsEmptyOptionalCellsAndFillsMissingCells(boolean update) throws Exception {
        var existingItem = new CodeListItemEntity().setId(10L).setCodeListId(7).setColumns(List.of("Alt", "ALT", ""));
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createStateList()));
        when(codeListItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        if (update) {
            when(codeListItemRepository.findByIdAndCodeListId(10L, 7)).thenReturn(Optional.of(existingItem));
        }

        var item = new CodeListItemEntity().setColumns(Arrays.asList(null, "BE"));
        var saved = update ? service.updateItem("test", 10L, item) : service.createItem("test", item);

        assertEquals(List.of("", "BE", ""), saved.getColumns());
        assertEquals(7, saved.getCodeListId());
    }

    // endregion

    // region Creating and updating lists

    @Test
    void createClearsClientProvidedId() throws Exception {
        when(codeListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(createManualCodeList().setId(0));

        assertNull(created.getId());
    }

    @Test
    void createRejectsReservedKey() {
        assertThrows(ResponseException.class, () -> service.create(createManualCodeList().setKey("new")));

        verify(codeListRepository, never()).save(any());
    }

    @Test
    void createRejectsKeyWithSlash() {
        assertThrows(ResponseException.class, () -> service.create(createManualCodeList().setKey("a/b")));

        verify(codeListRepository, never()).save(any());
    }

    @Test
    void createManualListStoresItsCellsUnderTheColumnKeys() throws Exception {
        when(codeListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(createManualCodeList()
                .setColumns(List.of())
                .setColumnMappings(List.of(
                        new CodeListColumnMapping(" Wert ", " Wert ", 2),
                        new CodeListColumnMapping("Beschriftung", "Beschriftung", 1)
                ))
                .setKeyColumnKey(" Wert "));

        assertEquals(List.of("Beschriftung", "Wert"), created.getColumns());
        assertEquals(new CodeListColumnMapping("Wert", "Wert", 2), created.getColumnMappings().getFirst());
        assertEquals("Wert", created.getKeyColumnKey());
    }

    @Test
    void updateOfAManualListAppendsNewColumnsAndFollowsRenamedKeys() throws Exception {
        var existing = createManualCodeList();
        when(codeListRepository.findById("test")).thenReturn(Optional.of(existing));
        when(codeListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = service.update("test", createManualCodeList()
                .setColumnMappings(List.of(
                        new CodeListColumnMapping("code", "Code", 1),
                        new CodeListColumnMapping("label", "Bezeichnung", 2),
                        new CodeListColumnMapping("extra", "Zusatz", 3)
                ))
                .setKeyColumnKey("code"));

        assertEquals(List.of("code", "label", "extra"), updated.getColumns());
        assertEquals("Bezeichnung", updated.getColumnMappings().get(1).name());
    }

    @Test
    void updateOfASynchronizedListKeepsTheStoredColumns() throws Exception {
        var existing = createAssetCodeList()
                .setColumns(List.of("id", "name"))
                .setColumnMappings(List.of(
                        new CodeListColumnMapping("id", "id", 1),
                        new CodeListColumnMapping("name", "name", 2)
                ))
                .setKeyColumnKey("id");
        when(codeListRepository.findById("test")).thenReturn(Optional.of(existing));
        when(codeListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = service.update("test", createAssetCodeList()
                .setColumns(List.of("manipulated"))
                .setColumnMappings(List.of(
                        new CodeListColumnMapping("identifier", "Kennung", 1),
                        new CodeListColumnMapping("name", "Name", 2)
                ))
                .setKeyColumnKey("identifier"));

        assertEquals(List.of("id", "name"), updated.getColumns());
        assertEquals(new CodeListColumnMapping("identifier", "Kennung", 1), updated.getColumnMappings().getFirst());
        assertEquals("identifier", updated.getKeyColumnKey());
        assertEquals(CodeListStatus.SyncPending, updated.getStatus());
    }

    @ParameterizedTest
    @MethodSource("invalidColumnMappings")
    void updateRejectsInvalidColumnMappings(List<CodeListColumnMapping> mappings, String keyColumnKey, String message) {
        var existing = createAssetCodeList().setColumns(List.of("id", "name"));
        when(codeListRepository.findById("test")).thenReturn(Optional.of(existing));

        var exception = assertThrows(ResponseException.class, () -> service.update("test", createAssetCodeList()
                .setColumnMappings(mappings)
                .setKeyColumnKey(keyColumnKey)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(message, exception.getMessage());
        verify(codeListRepository, never()).save(any());
    }

    private static Stream<Arguments> invalidColumnMappings() {
        var id = new CodeListColumnMapping("id", "id", 1);
        return Stream.of(
                Arguments.of(List.of(id, new CodeListColumnMapping(" ", "Name", 2)), "id", "Der Schlüssel einer Spalte darf nicht leer sein."),
                Arguments.of(List.of(id, new CodeListColumnMapping(" id ", "Name", 2)), "id", "Der Spaltenschlüssel „id“ ist mehrfach vergeben."),
                Arguments.of(List.of(id, new CodeListColumnMapping("name", "", 2)), "id", "Der Name der Spalte „name“ darf nicht leer sein."),
                Arguments.of(List.of(id, new CodeListColumnMapping("name", "Name", 3)), "id", "Die Position der Spalte „name“ ist ungültig."),
                Arguments.of(List.of(id, new CodeListColumnMapping("name", "Name", 0)), "id", "Die Position der Spalte „name“ ist ungültig."),
                Arguments.of(List.of(id, new CodeListColumnMapping("name", "Name", 1)), "id", "Die Position 1 ist mehreren Spalten zugeordnet."),
                Arguments.of(List.of(id), "unknown", "Die Schlüsselspalte muss eine vorhandene Spalte der Codeliste sein."),
                Arguments.of(List.of(id, new CodeListColumnMapping("removed", "Entfernt", null)), "removed", "Die Schlüsselspalte muss eine vorhandene Spalte der Codeliste sein.")
        );
    }

    @Test
    void manualListsNeedColumnsAndAKeyColumn() {
        var withoutColumns = assertThrows(ResponseException.class, () -> service.create(createManualCodeList().setColumnMappings(List.of())));
        var withoutKeyColumn = assertThrows(ResponseException.class, () -> service.create(createManualCodeList().setKeyColumnKey(" ")));

        assertEquals("Manuelle Codelisten benötigen mindestens eine Spalte.", withoutColumns.getMessage());
        assertEquals("Bitte wählen Sie eine Schlüsselspalte aus.", withoutKeyColumn.getMessage());
    }

    // endregion

    // region Proposals for new lists

    @Test
    void getXRepositoryColumnsProposesMappingsWithTheSourceKeyColumn() throws Exception {
        when(xRepositoryCodeListService.getCodeList("urn:test")).thenReturn(createXRepositoryCodeList());

        var proposal = service.getXRepositoryColumns("urn:test");

        assertEquals(new CodeListSourceColumnsDto(
                List.of("name", "code"),
                List.of(
                        new CodeListColumnMapping("name", "Bezeichnung", 1),
                        new CodeListColumnMapping("code", "Schlüssel", 2)
                ),
                "code"
        ), proposal);
    }

    @Test
    void getAssetColumnsProposesTheFirstColumnThatIdentifiesEveryRow() throws Exception {
        useCsvAsset("""
                group,,id
                1,x,a
                1,y,b
                """);

        var proposal = service.getAssetColumns(ASSET_KEY);

        assertEquals(List.of("group", "COL1", "id"), proposal.columns());
        assertEquals(new CodeListColumnMapping("COL1", "COL1", 2), proposal.columnMappings().get(1));
        assertEquals("COL1", proposal.keyColumnKey());
    }

    // endregion

    // region Synchronization

    @Test
    void firstSyncOfAnXRepositoryListCreatesMappingsAndReplacesItems() throws Exception {
        var current = createXRepositoryCodeList(List.of(), List.of());
        var claimedRun = claims(current);
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createXRepositoryCodeList(List.of(), List.of())));
        when(xRepositoryCodeListService.getCodeList("urn:test")).thenReturn(createXRepositoryCodeList(
                row(value("code", "001"), value("name", "Berlin")),
                row(value("code", "002"), value("name", "Hamburg"))
        ));

        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListRepository).claimSync(eq("test"), any(), any(), eq(STALE_BEFORE));
        verify(codeListItemRepository).deleteAllByCodeListId(7);
        verify(codeListRepository).save(current);
        assertEquals(CodeListStatus.Synced, current.getStatus());
        assertNull(current.getSyncRunId());
        assertNull(current.getSyncStarted());
        assertEquals(List.of("name", "code"), current.getColumns());
        assertEquals(List.of(
                new CodeListColumnMapping("name", "Bezeichnung", 1),
                new CodeListColumnMapping("code", "Schlüssel", 2)
        ), current.getColumnMappings());
        assertEquals("code", current.getKeyColumnKey());
        assertEquals(List.of(List.of("Berlin", "001"), List.of("Hamburg", "002")), savedItemColumns());
        verify(codeListRepository, never()).releaseSync(any(), any());
        assertEquals(1, claimedRun.size());
    }

    @Test
    void syncMarksDuplicateKeysFailed() throws Exception {
        var current = createXRepositoryCodeList(List.of(), List.of());
        var claimedRun = claims(current);
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createXRepositoryCodeList(List.of(), List.of())));
        when(xRepositoryCodeListService.getCodeList("urn:test")).thenReturn(createXRepositoryCodeList(
                row(value("code", "001"), value("name", "Berlin")),
                row(value("code", "001"), value("name", "Berlin duplicate"))
        ));
        when(codeListRepository.failSync(eq("test"), any(), any(), any())).thenReturn(1);

        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListItemRepository, never()).saveAll(any());
        verify(codeListRepository).failSync(eq("test"), eq(claimedRun.getFirst()),
                eq("Die Schlüsselspalte „Schlüssel“ enthält den Wert „001“ mehrfach."), any());
        verify(codeListRepository, never()).save(any());
        assertEquals(List.of(), current.getColumns());
    }

    @Test
    void firstSyncOfACsvUsesFallbackNamesForMissingHeaderColumns() throws Exception {
        var current = createAssetCodeList();
        claims(current);
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createAssetCodeList()));
        useCsvAsset("""
                code,,name
                001,x,Berlin
                002,x,Hamburg
                """);

        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListRepository).save(current);
        assertEquals(List.of("code", "COL1", "name"), current.getColumns());
        assertEquals("code", current.getKeyColumnKey());
        assertEquals(List.of(List.of("001", "x", "Berlin"), List.of("002", "x", "Hamburg")), savedItemColumns());
    }

    /** Test case 2 of OP#1265: the updated CSV adds the color column in front of the name column. */
    @Test
    void syncKeepsKeysAndNamesWhenTheSourceReordersItsColumns() throws Exception {
        var current = createSyncedGroupsList();
        claims(current);
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createSyncedGroupsList()));
        useCsvAsset("""
                id,color,name,alt_grouping
                1,red,Group 1,1
                2,blue,Group 2,1
                3,green,Group 3,2
                4,magenta,Group 4,3
                """);

        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        assertEquals(List.of("id", "color", "name", "alt_grouping"), current.getColumns());
        assertEquals(List.of(
                new CodeListColumnMapping("id", "id", 1),
                new CodeListColumnMapping("name", "Bezeichnung", 3),
                new CodeListColumnMapping("alt_grouping", "alt_grouping", 4),
                new CodeListColumnMapping("color", "color", 2)
        ), current.getColumnMappings());
        assertEquals("id", current.getKeyColumnKey());
        assertEquals(4, savedItemColumns().size());
    }

    @Test
    void syncKeepsOutdatedRowsInTheNewColumnOrder() throws Exception {
        var current = createSyncedGroupsList().setSyncKeepOutdated(true);
        claims(current);
        var updatedRow = new CodeListItemEntity().setId(1L).setCodeListId(7).setColumns(List.of("1", "Group 1", "1"));
        var outdatedRow = new CodeListItemEntity().setId(9L).setCodeListId(7).setColumns(List.of("9", "Group 9", "5"));
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createSyncedGroupsList().setSyncKeepOutdated(true)));
        when(codeListItemRepository.findAllByCodeListId(7)).thenReturn(List.of(updatedRow, outdatedRow));
        useCsvAsset("""
                id,color,name
                1,red,Group One
                4,magenta,Group 4
                """);

        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListItemRepository, never()).deleteAllByCodeListId(any());
        assertEquals(List.of(
                List.of("1", "red", "Group One"),
                List.of("9", "", "Group 9"),
                List.of("4", "magenta", "Group 4")
        ), savedItemColumns());
        assertEquals(new CodeListColumnMapping("alt_grouping", "alt_grouping", null), current.getColumnMappings().get(2));
    }

    @Test
    void syncRestoresTheMappingOfAColumnThatReturnsAndAvoidsKeyCollisions() throws Exception {
        var current = createAssetCodeList()
                .setColumns(List.of("id", "name"))
                .setColumnMappings(List.of(
                        new CodeListColumnMapping("id", "id", 1),
                        // Staff renamed the key of the name column to the name of a column that the source adds now.
                        new CodeListColumnMapping("label", "name", 2),
                        new CodeListColumnMapping("color", "Farbe", null)
                ))
                .setKeyColumnKey("id");
        claims(current);
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createAssetCodeList()));
        useCsvAsset("""
                id,name,label,color
                1,Group 1,G1,red
                """);

        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        assertEquals(List.of(
                new CodeListColumnMapping("id", "id", 1),
                new CodeListColumnMapping("label", "name", 2),
                new CodeListColumnMapping("color", "Farbe", 4),
                new CodeListColumnMapping("label_2", "label", 3)
        ), current.getColumnMappings());
    }

    @Test
    void syncUsesADefaultKeyColumnWhenTheKeyColumnIsGone() throws Exception {
        var current = createSyncedGroupsList();
        claims(current);
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createSyncedGroupsList()));
        useCsvAsset("""
                name,code
                Group 1,G1
                Group 1,G2
                """);

        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        assertEquals("code", current.getKeyColumnKey());
        assertNull(current.findColumnMapping("id").orElseThrow().index());
    }

    @Test
    void syncThatIsNotPendingDoesNothing() throws Exception {
        assertFalse(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListRepository, never()).findById(any());
        verify(codeListRepository, never()).save(any());
        verifyNoInteractions(xRepositoryCodeListService);
    }

    @Test
    void changeDuringTheSyncIsKeptAndRequestedAgain() throws Exception {
        var current = createXRepositoryCodeList(List.of(), List.of());
        var claimedRun = claims(current);
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createXRepositoryCodeList(List.of(), List.of())));
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
        var current = createXRepositoryCodeList(List.of(), List.of());
        claims(current);
        var otherRun = UUID.randomUUID();
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createXRepositoryCodeList(List.of(), List.of())));
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
        var claimedRun = claims(createXRepositoryCodeList(List.of(), List.of()));
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createXRepositoryCodeList(List.of(), List.of())));
        when(xRepositoryCodeListService.getCodeList("urn:test")).thenThrow(ResponseException.internalServerError("XRepository nicht erreichbar"));

        assertTrue(service.syncCodeList("test", STALE_BEFORE));

        verify(codeListRepository).failSync(eq("test"), eq(claimedRun.getFirst()), any(), any());
        verify(codeListRepository).releaseSync("test", claimedRun.getFirst());
    }

    @Test
    void requestSyncStoresTheRequestWithTheCodeList() {
        when(codeListRepository.requestSync(eq("test"), eq(true), any())).thenReturn(1);

        assertTrue(service.requestSync("test", true));
        assertFalse(service.requestSync("manual", false));
    }

    @Test
    void mergeColumnMappingsIgnoresMappingsThatPointBehindTheStoredColumns() {
        var mappings = CodeListService.mergeColumnMappings(
                List.of("id"),
                List.of(new CodeListColumnMapping("ghost", "Ghost", 5)),
                List.of("id"),
                Map.of()
        );

        assertEquals(List.of(
                new CodeListColumnMapping("ghost", "Ghost", null),
                new CodeListColumnMapping("id", "id", 1)
        ), mappings);
    }

    // endregion

    // region CSV export and import of manual lists

    @Test
    void exportCsvWritesColumnsAndAllCellsOfEveryItem() throws Exception {
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createManualCodeList()));
        when(codeListItemRepository.findAllByCodeListIdOrderByIdAsc(7)).thenReturn(List.of(
                new CodeListItemEntity().setId(1L).setCodeListId(7).setColumns(List.of("001", "Berlin")),
                new CodeListItemEntity().setId(2L).setCodeListId(7).setColumns(List.of("00,2", "Ham\"burg")),
                // The row was stored before the name column was added.
                new CodeListItemEntity().setId(3L).setCodeListId(7).setColumns(List.of("003"))
        ));

        var csv = new String(service.exportCSV("test"), StandardCharsets.UTF_8);

        assertEquals("code,name\r\n001,Berlin\r\n\"00,2\",\"Ham\"\"burg\"\r\n003,\r\n", csv);
    }

    @Test
    void importManualCsvReplacesItemsAndKeepsTheMappingsOfKnownColumns() throws Exception {
        var codeList = createManualCodeList();
        when(codeListRepository.findById("test")).thenReturn(Optional.of(codeList));
        when(codeListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var imported = service.importCSV("test", csv("""
                name,code,extra
                Berlin,001,x
                Hamburg,002,y
                """));

        verify(codeListItemRepository).deleteAllByCodeListId(7);
        assertEquals(List.of("name", "code", "extra"), imported.getColumns());
        assertEquals(List.of(
                new CodeListColumnMapping("code", "Code", 2),
                new CodeListColumnMapping("name", "Name", 1),
                new CodeListColumnMapping("extra", "extra", 3)
        ), imported.getColumnMappings());
        assertEquals("code", imported.getKeyColumnKey());
        assertEquals(List.of(List.of("Berlin", "001", "x"), List.of("Hamburg", "002", "y")), savedItemColumns());
    }

    @Test
    void importManualCsvRejectsDuplicateKeysBeforeDeletingItems() {
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createManualCodeList()));

        assertThrows(ResponseException.class, () -> service.importCSV("test", csv("""
                code,name
                001,Berlin
                001,Hamburg
                """)));

        verify(codeListItemRepository, never()).deleteAllByCodeListId(any());
        verify(codeListItemRepository, never()).saveAll(any());
    }

    @Test
    void importCsvRejectsNonManualCodeLists() {
        when(codeListRepository.findById("test")).thenReturn(Optional.of(createAssetCodeList()));

        assertThrows(ResponseException.class, () -> service.importCSV("test", csv("""
                code,name
                001,Berlin
                """)));

        verify(codeListItemRepository, never()).deleteAllByCodeListId(any());
        verify(codeListItemRepository, never()).saveAll(any());
    }

    // endregion

    private List<List<String>> savedItemColumns() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        var itemsCaptor = (ArgumentCaptor<Iterable<CodeListItemEntity>>) (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
        verify(codeListItemRepository).saveAll(itemsCaptor.capture());
        return StreamSupport
                .stream(itemsCaptor.getValue().spliterator(), false)
                .map(CodeListItemEntity::getColumns)
                .toList();
    }

    /** Claims the synchronization of the given list as the locked current state. Returns the claimed runs. */
    private List<UUID> claims(CodeListEntity current) {
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

    private void useCsvAsset(String content) throws Exception {
        var asset = new AssetEntity()
                .setKey(ASSET_KEY)
                .setPrivate(true)
                .setStorageProviderId(3)
                .setStoragePathFromRoot("/code-lists/test.csv");
        when(assetService.retrieve(ASSET_KEY)).thenReturn(Optional.of(asset));
        when(storageService.getDocumentContent(3, "/code-lists/test.csv")).thenReturn(csv(content));
    }

    private static ByteArrayInputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static PlatformTransactionManager transactionManager() {
        var transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        return transactionManager;
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
                .setColumnMappings(List.of(
                        new CodeListColumnMapping("code", "Code", 1),
                        new CodeListColumnMapping("name", "Name", 2)
                ))
                .setKeyColumnKey("code")
                .setStatus(CodeListStatus.Synced);
    }

    /** A manual list whose key column is the second of three columns. */
    private static CodeListEntity createStateList() {
        return createManualCodeList()
                .setColumns(List.of("name", "code", "extra"))
                .setColumnMappings(List.of(
                        new CodeListColumnMapping("name", "Anzeigename", 1),
                        new CodeListColumnMapping("code", "Schlüssel", 2),
                        new CodeListColumnMapping("extra", "Zusatz", 3)
                ));
    }

    private static CodeListEntity createXRepositoryCodeList(List<String> columns, List<CodeListColumnMapping> mappings) {
        return new CodeListEntity()
                .setKey("test")
                .setId(7)
                .setSourceType(CodeListSourceType.XRepository)
                .setSourceRef("urn:test")
                .setName("Test")
                .setDescription("")
                .setColumns(columns)
                .setColumnMappings(mappings)
                .setStatus(CodeListStatus.Syncing)
                .setSyncKeepOutdated(false);
    }

    private static CodeListEntity createAssetCodeList() {
        return new CodeListEntity()
                .setKey("test")
                .setId(7)
                .setSourceType(CodeListSourceType.Asset)
                .setSourceRef(ASSET_KEY.toString())
                .setName("Test")
                .setDescription("")
                .setColumns(List.of())
                .setColumnMappings(List.of())
                .setStatus(CodeListStatus.Syncing)
                .setSyncKeepOutdated(false);
    }

    /** The "Groups" list of OP#1265 after its initial synchronization; staff renamed the name column. */
    private static CodeListEntity createSyncedGroupsList() {
        return createAssetCodeList()
                .setColumns(List.of("id", "name", "alt_grouping"))
                .setColumnMappings(List.of(
                        new CodeListColumnMapping("id", "id", 1),
                        new CodeListColumnMapping("name", "Bezeichnung", 2),
                        new CodeListColumnMapping("alt_grouping", "alt_grouping", 3)
                ))
                .setKeyColumnKey("id");
    }

    private static XRepositoryCodeList createXRepositoryCodeList(XRepositoryCodeListSimpleCodeListRow... rows) {
        return new XRepositoryCodeList()
                .setColumnSet(new XRepositoryCodeListColumnSet()
                        .setColumn(List.of(
                                new XRepositoryCodeListColumnSetColumn().setId("name").setShortName("Bezeichnung"),
                                new XRepositoryCodeListColumnSetColumn().setId("code").setShortName("code").setLongName("Schlüssel")
                        ))
                        .setKey(new XRepositoryCodeListColumnSetKey()
                                .setColumnRef(new XRepositoryCodeListColumnSetKeyRef().setRef("code"))))
                .setCodeList(new XRepositoryCodeListSimpleCodeList()
                        .setRow(List.of(rows)));
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
