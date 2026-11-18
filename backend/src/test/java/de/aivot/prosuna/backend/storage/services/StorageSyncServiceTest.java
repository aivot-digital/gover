package de.aivot.prosuna.backend.storage.services;

import de.aivot.prosuna.backend.asset.entities.AssetEntity;
import de.aivot.prosuna.backend.asset.repositories.AssetRepository;
import de.aivot.prosuna.backend.storage.entities.StorageIndexItemEntity;
import de.aivot.prosuna.backend.storage.entities.StorageIndexItemEntityId;
import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.enums.StorageProviderStatus;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import de.aivot.prosuna.backend.storage.models.StorageDocument;
import de.aivot.prosuna.backend.storage.models.StorageFolder;
import de.aivot.prosuna.backend.storage.models.StorageItemMetadata;
import de.aivot.prosuna.backend.storage.models.StorageProviderDefinition;
import de.aivot.prosuna.backend.storage.projections.StorageIndexItemStateProjection;
import de.aivot.prosuna.backend.storage.repositories.StorageIndexItemRepository;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StorageSyncServiceTest {
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private static final Instant STALE_BEFORE = Instant.parse("2030-05-01T11:00:00Z");
    private static final Instant INDEXED = Instant.parse("2030-04-01T10:00:00Z");

    @Mock
    private KnownExtensionsService knownExtensions;

    @Mock
    private StorageProviderRepository storageProviderRepository;

    @Mock
    private StorageProviderDefinitionService storageProviderDefinitionService;

    @Mock
    private StorageProviderConfigurationService storageProviderConfigurationService;

    @Mock
    private StorageIndexItemRepository storageIndexItemRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private StorageProviderDefinition<Object> storageProviderDefinition;

    private StorageSyncService storageSyncService;

    private final List<SimpleTransactionStatus> transactions = new ArrayList<>();

    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenAnswer(call -> {
            var transaction = new SimpleTransactionStatus();
            transactions.add(transaction);
            return transaction;
        });
        storageSyncService = new StorageSyncService(
                knownExtensions,
                storageProviderRepository,
                storageProviderDefinitionService,
                storageProviderConfigurationService,
                storageIndexItemRepository,
                assetRepository,
                transactionManager,
                meterRegistry
        );
    }

    @Test
    void syncThatCannotBeClaimedDoesNothing() {
        assertFalse(storageSyncService.syncRequestedStorageProvider(7, STALE_BEFORE));

        verify(storageProviderRepository).claimSync(eq(7), any(), any(), eq(STALE_BEFORE));
        verify(storageProviderRepository, never()).findById(any());
        verify(storageProviderRepository, never()).save(any());
        verifyNoInteractions(storageProviderDefinitionService);
    }

    @Test
    void requestedSyncOfADeletedProviderDoesNothing() {
        claims(new StorageProviderEntity().setId(7));
        when(storageProviderRepository.findById(7)).thenReturn(Optional.empty());

        assertFalse(storageSyncService.syncRequestedStorageProvider(7, STALE_BEFORE));

        verify(storageProviderRepository, never()).save(any());
    }

    @Test
    void failedSynchronizationIsRecordedByTheClaimingRun() {
        var current = provider("missing");
        claims(current);
        when(storageProviderRepository.findById(7)).thenReturn(Optional.of(provider("missing")));

        assertFalse(storageSyncService.syncRequestedStorageProvider(7, STALE_BEFORE));

        // The missing definition fails the synchronization, which shows that it ran.
        assertEquals(StorageProviderStatus.SyncFailed, current.getStatus());
        assertNotNull(current.getStatusMessage());
        assertNull(current.getSyncRunId());
        verify(storageProviderRepository).save(current);
        assertFalse(transactions.getFirst().isRollbackOnly());

        assertEquals(1, meterRegistry.counter("prosuna.sync.runs", "type", "storage", "outcome", "failed").count());
        assertEquals(0, meterRegistry.counter("prosuna.sync.runs", "type", "storage", "outcome", "success").count());
    }

    @Test
    void syncRunsAtReadCommitted() {
        claims(provider("missing"));
        when(storageProviderRepository.findById(7)).thenReturn(Optional.of(provider("missing")));

        storageSyncService.syncRequestedStorageProvider(7, STALE_BEFORE);

        var definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(definition.capture());
        assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getValue().getIsolationLevel());
    }

    @Test
    void newerRequestDuringTheSyncRollsBackTheRunAndIsPublishedAgain() {
        var current = provider("test");
        var runs = claims(current);
        when(storageProviderRepository.findById(7)).thenReturn(Optional.of(provider("test")));
        when(storageProviderDefinitionService.retrieveProviderDefinition("test", 1)).thenAnswer(call -> {
            // The provider is requested again while its storage is read.
            current.setStatus(StorageProviderStatus.SyncPending);
            return Optional.empty();
        });

        assertTrue(storageSyncService.syncRequestedStorageProvider(7, STALE_BEFORE));

        assertTrue(transactions.getFirst().isRollbackOnly());
        assertEquals(StorageProviderStatus.SyncPending, current.getStatus());
        verify(storageProviderRepository, never()).save(any());
        verify(storageProviderRepository).releaseSync(7, runs.getFirst());
    }

    @Test
    void runThatWasTakenOverRollsBack() {
        var current = provider("missing");
        claims(current);
        var otherRun = UUID.randomUUID();
        when(storageProviderRepository.findById(7)).thenAnswer(call -> {
            current.setSyncRunId(otherRun);
            return Optional.of(provider("missing"));
        });

        storageSyncService.syncRequestedStorageProvider(7, STALE_BEFORE);

        assertTrue(transactions.getFirst().isRollbackOnly());
        assertEquals(otherRun, current.getSyncRunId());
        verify(storageProviderRepository, never()).save(any());
    }

    @Test
    void databaseFailureIsRecordedAfterTheRollback() {
        var current = provider("test");
        var runs = claims(current);
        when(storageProviderRepository.findById(7)).thenReturn(Optional.of(provider("test")));
        when(storageProviderDefinitionService.retrieveProviderDefinition("test", 1)).thenAnswer(call -> {
            transactions.getFirst().setRollbackOnly();
            throw new QueryTimeoutException("database busy");
        });
        when(storageProviderRepository.failSync(eq(7), any(), any(), any())).thenReturn(1);

        assertFalse(storageSyncService.syncRequestedStorageProvider(7, STALE_BEFORE));

        verify(storageProviderRepository).failSync(eq(7), eq(runs.getFirst()), eq("database busy"), any());
        verify(storageProviderRepository, never()).findByIdForUpdate(any());
        verify(storageProviderRepository, never()).save(any());
    }

    @Test
    void syncUpdatesIndexItemTimestampsFromProvider() throws Exception {
        var current = provider("test");
        claims(current);
        var provider = new StorageProviderEntity()
                .setId(7)
                .setName("Provider")
                .setStorageProviderDefinitionKey("test")
                .setStorageProviderDefinitionVersion(1)
                .setType(StorageProviderType.Attachments)
                .setMetadataAttributes(List.of());
        var config = new Object();
        var remoteCreated = Instant.parse("2026-01-02T03:04:05Z");
        var remoteUpdated = Instant.parse("2026-01-03T04:05:06Z");
        var existingCreated = Instant.parse("2025-01-02T03:04:05Z");
        var existingUpdated = Instant.parse("2025-01-03T04:05:06Z");

        var document = new StorageDocument("/file.txt", "file.txt", 5L, StorageItemMetadata.empty());
        document
                .setCreated(remoteCreated)
                .setUpdated(remoteUpdated);
        var root = new StorageFolder("/", "Root", new LinkedList<>(), new LinkedList<>(), true)
                .addDocument(document);

        var existingRoot = new StorageIndexItemEntity(
                provider.getId(),
                provider.getType(),
                "/",
                true,
                "Root",
                0L,
                StorageService.FOLDER_MIME_TYPE,
                false,
                StorageItemMetadata.empty(),
                existingCreated,
                existingUpdated
        );
        var existingDocument = new StorageIndexItemEntity(
                provider.getId(),
                provider.getType(),
                "/file.txt",
                false,
                "file.txt",
                5L,
                "text/plain",
                false,
                StorageItemMetadata.empty(),
                existingCreated,
                existingUpdated
        );

        when(storageProviderDefinitionService.retrieveProviderDefinition("test", 1)).thenReturn(Optional.of(storageProviderDefinition));
        when(storageProviderConfigurationService.mapToConfig(provider, storageProviderDefinition)).thenReturn(config);
        when(storageProviderDefinition.rootFolder(config, true)).thenReturn(root);
        when(storageProviderDefinition.getSupportsMetadataAttributes()).thenReturn(false);
        when(knownExtensions.determineMimeType("file.txt")).thenReturn(Optional.of("text/plain"));
        when(storageIndexItemRepository.findById(any())).thenAnswer(invocation -> {
            var id = invocation.getArgument(0, StorageIndexItemEntityId.class);
            if ("/".equals(id.getPathFromRoot())) {
                return Optional.of(existingRoot);
            }
            if ("/file.txt".equals(id.getPathFromRoot())) {
                return Optional.of(existingDocument);
            }
            return Optional.empty();
        });
        when(storageIndexItemRepository.findStatesByStorageProviderId(provider.getId())).thenReturn(List.of(
                new StorageIndexItemStateProjection("/", true, existingUpdated),
                new StorageIndexItemStateProjection("/file.txt", false, existingUpdated)
        ));
        when(storageProviderRepository.findById(7)).thenReturn(Optional.of(provider));

        assertFalse(storageSyncService.syncRequestedStorageProvider(7, STALE_BEFORE));

        assertEquals(StorageProviderStatus.Synced, current.getStatus());
        assertNotNull(current.getLastSync());
        assertNull(current.getSyncRunId());
        verify(storageProviderRepository).save(current);

        var indexItemCaptor = ArgumentCaptor.forClass(StorageIndexItemEntity.class);
        verify(storageIndexItemRepository).save(indexItemCaptor.capture());

        var savedItem = indexItemCaptor.getValue();
        assertEquals("/file.txt", savedItem.getPathFromRoot());
        assertEquals(remoteCreated, savedItem.getCreated());
        assertEquals(remoteUpdated, savedItem.getUpdated());
    }

    @Test
    void uploadCommittedDuringTheSyncIsNotMarkedMissing() throws Exception {
        var current = provider("test");
        claims(current);
        var listed = new AtomicBoolean();
        syncs(assetProvider(), call -> {
            listed.set(true);
            return emptyRoot();
        });
        var root = new StorageIndexItemStateProjection("/", true, INDEXED);
        var upload = new StorageIndexItemStateProjection("/upload.pdf", false, Instant.parse("2030-05-01T12:00:00Z"));
        // At READ COMMITTED a read after the listing sees the upload, which the listing misses.
        when(storageIndexItemRepository.findStatesByStorageProviderId(7))
                .thenAnswer(call -> listed.get() ? List.of(root, upload) : List.of(root));

        assertFalse(storageSyncService.syncRequestedStorageProvider(7, STALE_BEFORE));

        assertEquals(StorageProviderStatus.Synced, current.getStatus());
        verify(storageIndexItemRepository, never()).markMissingIfUnchanged(any(), any(), any(), any());
        verify(assetRepository, never()).delete(any(AssetEntity.class));
    }

    @Test
    void onlyItemsUnchangedSinceTheListingAreMarkedMissing() throws Exception {
        var current = provider("test");
        claims(current);
        syncs(assetProvider(), call -> emptyRoot());
        when(storageIndexItemRepository.findStatesByStorageProviderId(7)).thenReturn(List.of(
                new StorageIndexItemStateProjection("/", true, INDEXED),
                new StorageIndexItemStateProjection("/deleted.pdf", false, INDEXED),
                new StorageIndexItemStateProjection("/replaced.pdf", false, INDEXED)
        ));
        // "/replaced.pdf" was uploaded again during the run, so its conditional update matches no row.
        when(storageIndexItemRepository.markMissingIfUnchanged(eq(7), eq("/deleted.pdf"), eq(INDEXED), any())).thenReturn(1);
        var deletedAsset = new AssetEntity();
        when(assetRepository.findByStorageProviderIdAndStoragePathFromRoot(7, "/deleted.pdf")).thenReturn(Optional.of(deletedAsset));

        assertFalse(storageSyncService.syncRequestedStorageProvider(7, STALE_BEFORE));

        assertEquals(StorageProviderStatus.Synced, current.getStatus());
        verify(storageIndexItemRepository).markMissingIfUnchanged(eq(7), eq("/replaced.pdf"), eq(INDEXED), any());
        verify(storageIndexItemRepository, never()).markMissingIfUnchanged(any(), eq("/"), any(), any());
        verify(assetRepository).delete(deletedAsset);
        verify(assetRepository, never()).findByStorageProviderIdAndStoragePathFromRoot(7, "/replaced.pdf");
    }

    /** Lets provider 7 sync with the given storage listing. */
    private void syncs(StorageProviderEntity provider, Answer<StorageFolder> listing) throws Exception {
        var config = new Object();
        when(storageProviderRepository.findById(7)).thenReturn(Optional.of(provider));
        when(storageProviderDefinitionService.retrieveProviderDefinition("test", 1)).thenReturn(Optional.of(storageProviderDefinition));
        when(storageProviderConfigurationService.mapToConfig(provider, storageProviderDefinition)).thenReturn(config);
        when(storageProviderDefinition.rootFolder(config, true)).thenAnswer(listing);
        when(storageProviderDefinition.getSupportsMetadataAttributes()).thenReturn(false);
    }

    private static StorageProviderEntity assetProvider() {
        return provider("test").setType(StorageProviderType.Assets);
    }

    private static StorageFolder emptyRoot() {
        return new StorageFolder("/", "Root", new LinkedList<>(), new LinkedList<>(), true);
    }

    /** Claims the synchronization of provider 7 as the locked current state. Returns the claimed runs. */
    private List<UUID> claims(StorageProviderEntity current) {
        var runs = new ArrayList<UUID>();
        when(storageProviderRepository.claimSync(eq(7), any(), any(), any())).thenAnswer(call -> {
            UUID runId = call.getArgument(1);
            runs.add(runId);
            current.setStatus(StorageProviderStatus.Syncing).setSyncRunId(runId).setSyncStarted(call.getArgument(2));
            return 1;
        });
        when(storageProviderRepository.findByIdForUpdate(7)).thenReturn(Optional.of(current));
        return runs;
    }

    private static StorageProviderEntity provider(String definitionKey) {
        return new StorageProviderEntity()
                .setId(7)
                .setName("Ablage")
                .setStatus(StorageProviderStatus.Syncing)
                .setStorageProviderDefinitionKey(definitionKey)
                .setStorageProviderDefinitionVersion(1)
                .setType(StorageProviderType.Attachments)
                .setMetadataAttributes(List.of());
    }
}
