package de.aivot.prosuna.backend.search.repositories;

import de.aivot.prosuna.backend.search.entities.SearchRecentItemEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SearchRecentItemRepository extends JpaRepository<SearchRecentItemEntity, Long> {
    List<SearchRecentItemEntity> findAllByUserIdAndLastAccessedGreaterThanEqual(
            String userId,
            Instant lastAccessed,
            Pageable pageable
    );

    @Modifying
    @Query(value = """
            insert into search_recent_items (user_id, origin_table, item_id, created, last_accessed)
            values (:userId, :originTable, :itemId, current_timestamp, current_timestamp)
            on conflict (user_id, origin_table, item_id)
                do update set last_accessed = excluded.last_accessed
            """, nativeQuery = true)
    void upsert(
            @Param("userId") String userId,
            @Param("originTable") String originTable,
            @Param("itemId") String itemId
    );

    @Modifying
    @Query(value = """
            delete from search_recent_items
            where id in (
                select id
                from (
                    select id,
                           row_number() over (partition by user_id order by last_accessed desc, id desc) as row_index
                    from search_recent_items
                    where user_id = :userId
                ) ranked_items
                where row_index > :maxItems
            )
            """, nativeQuery = true)
    int deleteOverflow(
            @Param("userId") String userId,
            @Param("maxItems") int maxItems
    );

    int deleteAllByLastAccessedBefore(Instant cutoff);
}
