package de.aivot.prosuna.backend.seeding.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.seeding.models.SeedProvider;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
public class SeedingService implements SmartInitializingSingleton {
    private static final Logger logger = LoggerFactory.getLogger(SeedingService.class);

    private final List<SeedProvider> seedProviders;
    private final TransactionTemplate transaction;
    private final EntityManager entityManager;

    public SeedingService(List<SeedProvider> seedProviders,
                          PlatformTransactionManager transactionManager,
                          EntityManager entityManager) {
        this.seedProviders = seedProviders;
        this.transaction = new TransactionTemplate(transactionManager);
        // Seeds check for existing data after waiting for the seeding lock. Under REPEATABLE READ, their snapshot would
        // predate the commit of an instance that seeded in the meantime, so they would seed the data a second time.
        this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.entityManager = entityManager;
    }

    /**
     * Seeds after Flyway migrated the database, but before the web server and the scheduled jobs start.
     * Otherwise, a user import could reference the default system role before it exists.
     */
    @Override
    public void afterSingletonsInstantiated() {
        seedAll();
    }

    public void seedAll() {
        // A single transaction keeps a failed seed from leaving a partially seeded installation behind.
        transaction.executeWithoutResult(status -> {
            // Instances starting at the same time against a fresh database would otherwise both seed the data.
            entityManager
                    .createNativeQuery("select 1 from pg_advisory_xact_lock(hashtext('prosuna.seeding'))")
                    .getSingleResult();

            for (var seedProvider : seedProviders) {
                seed(seedProvider);
            }
        });
    }

    private void seed(@Nonnull SeedProvider seedProvider) {
        if (!seedProvider.shouldSeed()) {
            logger.info("Skipping seed {} because its data already exists", seedProvider.getClass().getSimpleName());
            return;
        }

        logger.info("Seeding {}", seedProvider.getClass().getSimpleName());
        try {
            seedProvider.seed();
        } catch (ResponseException e) {
            // Abort the startup, so the installation does not run without its initial data.
            throw new IllegalStateException("Failed to seed " + seedProvider.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
