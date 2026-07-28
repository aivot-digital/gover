package de.aivot.gover.backend.search.services;

import de.aivot.gover.backend.search.dtos.SearchItemResponseDTO;
import de.aivot.gover.backend.search.dtos.SearchRecentItemRequestDTO;
import de.aivot.gover.backend.search.entities.SearchRecentItemEntity;
import de.aivot.gover.backend.search.repositories.SearchRecentItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchRecentItemServiceTest {
    @Test
    void listVisibleRecentItemsFiltersItemsByCurrentPermissions() {
        var pageableReference = new AtomicReference<Pageable>();
        var cutoffReference = new AtomicReference<Instant>();
        var repository = createRepositoryProxy((methodName, args) -> switch (methodName) {
            case "findAllByUserIdAndLastAccessedGreaterThanEqual" -> {
                cutoffReference.set((Instant) args[1]);
                pageableReference.set((Pageable) args[2]);
                yield List.of(
                        recentItem("processes", "process-1,1"),
                        recentItem("secrets", "secret-1"),
                        recentItem("departments", "department-1")
                );
            }
            default -> throw new AssertionError("Unexpected repository method call: " + methodName);
        });
        var searchItemService = new TestSearchItemService(Map.of(
                "processes:process-1,1", new SearchItemResponseDTO("process-1,1", "Prozess 1", "processes"),
                "departments:department-1", new SearchItemResponseDTO("department-1", "Abteilung 1", "departments")
        ));
        var service = new SearchRecentItemService(repository, searchItemService, 50, 180);

        var result = service.listVisibleRecentItems("user-1", 10);

        assertEquals(
                List.of(
                        new SearchItemResponseDTO("process-1,1", "Prozess 1", "processes"),
                        new SearchItemResponseDTO("department-1", "Abteilung 1", "departments")
                ),
                result
        );
        assertEquals(50, pageableReference.get().getPageSize());
        assertTrue(cutoffReference.get().isBefore(Instant.now()));
        assertEquals(List.of(
                "user-1:processes:process-1,1",
                "user-1:secrets:secret-1",
                "user-1:departments:department-1"
        ), searchItemService.lookups);
    }

    @Test
    void recordRecentItemStoresOnlyCurrentlyVisibleItems() {
        var upsertCount = new AtomicInteger();
        var overflowCleanupCount = new AtomicInteger();
        var upsertedItem = new AtomicReference<List<String>>();
        var repository = createRepositoryProxy((methodName, args) -> switch (methodName) {
            case "upsert" -> {
                upsertCount.incrementAndGet();
                upsertedItem.set(List.of((String) args[0], (String) args[1], (String) args[2]));
                yield null;
            }
            case "deleteOverflow" -> {
                overflowCleanupCount.incrementAndGet();
                yield 0;
            }
            default -> throw new AssertionError("Unexpected repository method call: " + methodName);
        });
        var searchItemService = new TestSearchItemService(Map.of(
                "secrets:secret-1", new SearchItemResponseDTO("secret-1", "Geheimnis 1", "secrets")
        ));
        var service = new SearchRecentItemService(repository, searchItemService, 25, 180);

        service.recordRecentItem("user-1", new SearchRecentItemRequestDTO("secret-1", "secrets"));
        service.recordRecentItem("user-1", new SearchRecentItemRequestDTO("secret-2", "secrets"));

        assertEquals(1, upsertCount.get());
        assertEquals(1, overflowCleanupCount.get());
        assertEquals(List.of("user-1", "secrets", "secret-1"), upsertedItem.get());
        assertEquals(List.of(
                "user-1:secrets:secret-1",
                "user-1:secrets:secret-2"
        ), searchItemService.lookups);
    }

    private interface ProxyHandler {
        Object invoke(String methodName, Object[] args);
    }

    private static SearchRecentItemRepository createRepositoryProxy(ProxyHandler handler) {
        return (SearchRecentItemRepository) Proxy.newProxyInstance(
                SearchRecentItemRepository.class.getClassLoader(),
                new Class<?>[]{SearchRecentItemRepository.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> SearchRecentItemRepository.class.getSimpleName() + "Proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }

                    return handler.invoke(method.getName(), args);
                }
        );
    }

    private static SearchRecentItemEntity recentItem(String originTable, String itemId) {
        return new SearchRecentItemEntity()
                .setUserId("user-1")
                .setOriginTable(originTable)
                .setItemId(itemId)
                .setCreated(Instant.now())
                .setLastAccessed(Instant.now());
    }

    private static final class TestSearchItemService extends SearchItemService {
        private final Map<String, SearchItemResponseDTO> visibleItems;
        private final List<String> lookups = new ArrayList<>();

        private TestSearchItemService(Map<String, SearchItemResponseDTO> visibleItems) {
            super(null, List.of());
            this.visibleItems = visibleItems;
        }

        @Override
        public Optional<SearchItemResponseDTO> retrieveVisible(String userId, String originTable, String id) {
            lookups.add("%s:%s:%s".formatted(userId, originTable, id));
            return Optional.ofNullable(visibleItems.get("%s:%s".formatted(originTable, id)));
        }
    }
}
