package de.aivot.gover.backend.search.services;

import de.aivot.gover.backend.search.dtos.SearchItemResponseDTO;
import de.aivot.gover.backend.search.dtos.SearchRecentItemRequestDTO;
import de.aivot.gover.backend.search.properties.SearchRecentItemProperties;
import de.aivot.gover.backend.search.repositories.SearchRecentItemRepository;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@EnableScheduling
public class SearchRecentItemService {
    private static final Logger logger = LoggerFactory.getLogger(SearchRecentItemService.class);

    private final SearchRecentItemRepository searchRecentItemRepository;
    private final SearchItemService searchItemService;
    private final SearchRecentItemProperties properties;
    private final AtomicBoolean cleanupRunning = new AtomicBoolean(false);

    public SearchRecentItemService(SearchRecentItemRepository searchRecentItemRepository,
                                   SearchItemService searchItemService,
                                   SearchRecentItemProperties properties) {
        this.searchRecentItemRepository = searchRecentItemRepository;
        this.searchItemService = searchItemService;
        this.properties = properties;
    }

    @Nonnull
    public List<SearchItemResponseDTO> listVisibleRecentItems(
            @Nonnull String userId,
            int size
    ) {
        var normalizedSize = Math.clamp(size, 1, properties.getMaxItemsPerUser());
        var cutoff = Instant.now().minus(properties.getRetentionDays(), ChronoUnit.DAYS);
        var recentItems = searchRecentItemRepository
                .findAllByUserIdAndLastAccessedGreaterThanEqual(
                        userId,
                        cutoff,
                        PageRequest.of(
                                0,
                                properties.getMaxItemsPerUser(),
                                Sort.by(Sort.Order.desc("lastAccessed"), Sort.Order.desc("id"))
                        )
        );
        var result = new ArrayList<SearchItemResponseDTO>();

        for (var recentItem : recentItems) {
            // Resolve every stored row through the search view so revoked permissions hide stale entries immediately.
            searchItemService
                    .retrieveVisible(userId, recentItem.getOriginTable(), recentItem.getItemId())
                    .ifPresent(result::add);

            if (result.size() >= normalizedSize) {
                break;
            }
        }

        return result;
    }

    @Transactional
    public void recordRecentItem(
            @Nonnull String userId,
            @Nonnull SearchRecentItemRequestDTO request
    ) {
        // Ignore stale or forged client requests; only currently visible search items may be persisted.
        var visibleItem = searchItemService
                .retrieveVisible(userId, request.originTable(), request.id());

        if (visibleItem.isEmpty()) {
            return;
        }

        searchRecentItemRepository.upsert(
                userId,
                request.originTable(),
                request.id()
        );
        searchRecentItemRepository.deleteOverflow(userId, properties.getMaxItemsPerUser());
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void deleteExpiredRecentItemsOnStartup() {
        deleteExpiredRecentItems("startup");
    }

    @Transactional
    @Scheduled(cron = "0 30 3 * * *", zone = "${gover.timezone}")
    public void deleteExpiredRecentItemsNightly() {
        deleteExpiredRecentItems("daily-schedule");
    }

    @Transactional
    public void deleteExpiredRecentItems(@Nonnull String trigger) {
        if (!cleanupRunning.compareAndSet(false, true)) {
            logger.info("Skipping recent search item cleanup because another run is still active.");
            return;
        }

        try {
            var cutoff = Instant.now().minus(properties.getRetentionDays(), ChronoUnit.DAYS);
            var deletedCount = searchRecentItemRepository.deleteAllByLastAccessedBefore(cutoff);

            if (deletedCount > 0) {
                logger.info(
                        "Deleted {} recent search item(s) older than {} day(s) (trigger={}).",
                        deletedCount,
                        properties.getRetentionDays(),
                        trigger
                );
            }
        } finally {
            cleanupRunning.set(false);
        }
    }
}
