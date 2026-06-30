package de.aivot.gover.backend.search.repositories;

import de.aivot.gover.backend.core.repositories.ReadOnlyRepository;
import de.aivot.gover.backend.search.entities.SearchItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchEntityRepository extends ReadOnlyRepository<SearchItemEntity, String>, JpaSpecificationExecutor<SearchItemEntity> {
    @Query(
            value = """
                        SELECT
                            word_similarity(search_text, :search) as sim,
                            *
                        FROM
                            v_search_items
                        WHERE
                            (origin_table <> 'process_nodes' OR origin_table_subset in :process_node_origin_table_subset) AND
                            word_similarity(search_text, :search) > 0.1
                        ORDER BY
                            sim DESC;
                    """, nativeQuery = true
    )
    Page<SearchItemEntity> search(@Param("search") String search,
                                  @Param("process_node_origin_table_subset") String[] allowedProcessNodeOriginTableSubset,
                                  Pageable pageable);

    @Query(
            value = """
                        SELECT
                            word_similarity(search_text, :search) as sim,
                            *
                        FROM
                            v_search_items
                        WHERE
                            (origin_table <> 'process_nodes' OR origin_table_subset in :process_node_origin_table_subset) AND
                            word_similarity(search_text, :search) > 0.1 AND
                            origin_table = :originTable
                        ORDER BY
                            sim DESC;
                    """, nativeQuery = true
    )
    Page<SearchItemEntity> search(@Param("search") String search,
                                  @Param("originTable") String originTable,
                                  @Param("process_node_origin_table_subset") String[] allowedProcessNodeOriginTableSubset,
                                  Pageable pageable);
}
