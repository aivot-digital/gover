package de.aivot.GoverBackend.search.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.search.entities.SearchItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchEntityRepository extends ReadOnlyRepository<SearchItemEntity, String>, JpaSpecificationExecutor<SearchItemEntity> {
    /*
    @Query(
            value = """
                        SELECT
                            *, similarity(label, :search) AS sim
                        FROM
                            search_items
                        WHERE
                            label IS NOT NULL AND label <> ''
                        ORDER BY
                            sim DESC;
                    """, nativeQuery = true
    )
     */
    @Query(
            value = """
                        SELECT
                            word_similarity(search_text, :search) as sim,
                            *
                        FROM
                            search_items
                        WHERE
                            word_similarity(search_text, :search) > 0.05
                        ORDER BY
                            sim DESC;
                    """, nativeQuery = true
    )
    Page<SearchItemEntity> search(@Param("search") String search, Pageable pageable);
}
