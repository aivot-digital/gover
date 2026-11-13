package de.aivot.prosuna.backend.seeding.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.seeding.models.SeedProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeedingServiceTest {
    private static SeedProvider mockSeedProvider(boolean shouldSeed) {
        var seedProvider = mock(SeedProvider.class);
        when(seedProvider.shouldSeed()).thenReturn(shouldSeed);
        return seedProvider;
    }

    private static EntityManager mockEntityManager() {
        var entityManager = mock(EntityManager.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mock(Query.class));
        return entityManager;
    }

    @Test
    void seedAllShouldSeedProvidersWhoseDataIsMissing() throws ResponseException {
        var seedProvider = mockSeedProvider(true);
        var service = new SeedingService(List.of(seedProvider), mock(PlatformTransactionManager.class), mockEntityManager());

        service.seedAll();

        verify(seedProvider).seed();
    }

    @Test
    void seedAllShouldSkipProvidersWhoseDataExists() throws ResponseException {
        var seedProvider = mockSeedProvider(false);
        var service = new SeedingService(List.of(seedProvider), mock(PlatformTransactionManager.class), mockEntityManager());

        service.seedAll();

        verify(seedProvider, never()).seed();
    }

    @Test
    void seedAllShouldLockBeforeCheckingDataInReadCommittedTransaction() throws ResponseException {
        var seedProvider = mockSeedProvider(true);
        var transactionManager = mock(PlatformTransactionManager.class);
        var entityManager = mockEntityManager();
        var service = new SeedingService(List.of(seedProvider), transactionManager, entityManager);

        service.seedAll();

        var definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(definition.capture());
        assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, definition.getValue().getIsolationLevel());

        var order = inOrder(entityManager, seedProvider, transactionManager);
        order.verify(entityManager).createNativeQuery("select 1 from pg_advisory_xact_lock(hashtext('prosuna.seeding'))");
        order.verify(seedProvider).shouldSeed();
        order.verify(seedProvider).seed();
        order.verify(transactionManager).commit(any());
    }

    @Test
    void seedAllShouldAbortAndRollBackWhenSeedFails() throws ResponseException {
        var failingSeedProvider = mockSeedProvider(true);
        var failure = ResponseException.badRequest("Die folgenden Berechtigungen sind im System nicht bekannt: unknown.");
        doThrow(failure).when(failingSeedProvider).seed();
        var nextSeedProvider = mockSeedProvider(true);
        var transactionManager = mock(PlatformTransactionManager.class);
        var service = new SeedingService(
                List.of(failingSeedProvider, nextSeedProvider),
                transactionManager,
                mockEntityManager()
        );

        var exception = assertThrows(IllegalStateException.class, service::seedAll);

        assertSame(failure, exception.getCause());
        verify(nextSeedProvider, never()).seed();
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
    }
}
