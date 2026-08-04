package de.aivot.gover.backend.search.services;

import de.aivot.gover.backend.search.dtos.SearchItemResponseDTO;
import de.aivot.gover.backend.search.dtos.SearchRecentItemRequestDTO;
import de.aivot.gover.backend.search.repositories.SearchRecentItemRepository;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private static final int DEFAULT_MAX_ITEMS_PER_USER = 50;
    private static final int DEFAULT_RETENTION_DAYS = 90;

    private final SearchRecentItemRepository searchRecentItemRepository;
    private final SearchItemService searchItemService;
    private final int maxItemsPerUser;
    private final int retentionDays;
    private final AtomicBoolean cleanupRunning = new AtomicBoolean(false);

    public SearchRecentItemService(SearchRecentItemRepository searchRecentItemRepository,
                                   SearchItemService searchItemService,
                                   @Value("${gover.search.recent.max-items-per-user:50}") Integer maxItemsPerUser,
                                   @Value("${gover.search.recent.retention-days:90}") Integer retentionDays) {
        this.searchRecentItemRepository = searchRecentItemRepository;
        this.searchItemService = searchItemService;
        this.maxItemsPerUser = maxItemsPerUser != null && maxItemsPerUser > 0
                ? maxItemsPerUser
                : DEFAULT_MAX_ITEMS_PER_USER;
        this.retentionDays = retentionDays != null && retentionDays > 0
                ? retentionDays
                : DEFAULT_RETENTION_DAYS;
    }

    @Nonnull
    public List<SearchItemResponseDTO> listVisibleRecentItems(
            @Nonnull String userId,
            int size
    ) {
        var normalizedSize = Math.clamp(size, 1, maxItemsPerUser);
        var cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        var recentItems = searchRecentItemRepository
                .findAllByUserIdAndLastAccessedGreaterThanEqual(
                        userId,
                        cutoff,
                        PageRequest.of(
                                0,
                                maxItemsPerUser,
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
        searchRecentItemRepository.deleteOverflow(userId, maxItemsPerUser);
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
            var cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
            var deletedCount = searchRecentItemRepository.deleteAllByLastAccessedBefore(cutoff);

            if (deletedCount > 0) {
                logger.info(
                        "Deleted {} recent search item(s) older than {} day(s) (trigger={}).",
                        deletedCount,
                        retentionDays,
                        trigger
                );
            }
        } finally {
            cleanupRunning.set(false);
        }
    }
}
