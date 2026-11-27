package de.aivot.prosuna.backend.codeLists.repositories;

import de.aivot.prosuna.backend.codeLists.entities.CodeListItemEntity;
import de.aivot.prosuna.backend.codeLists.models.CodeListItemSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CodeListItemRepository extends JpaRepository<CodeListItemEntity, Long>, JpaSpecificationExecutor<CodeListItemEntity> {
    List<CodeListItemEntity> findAllByCodeListId(Integer codeListId);

    Page<CodeListItemEntity> findAllByCodeListId(Integer codeListId, Pageable pageable);

    List<CodeListItemEntity> findAllByCodeListIdOrderByIdAsc(Integer codeListId);

    Optional<CodeListItemEntity> findByIdAndCodeListId(Long id, Integer codeListId);

    /**
     * Lists the rows of a list with the columns the list had when they were read. Both come from one statement, so a
     * synchronization that commits in between cannot pair one column order with the cells of another. The selected
     * values are not cached entities, so this also holds within a transaction that loaded the rows before.
     */
    @Query("""
            select new de.aivot.prosuna.backend.codeLists.models.CodeListItemSnapshot(
                item.id, item.codeListId, item.columns, item.created, item.updated, codeList.columns)
            from CodeListItemEntity item
            join CodeListEntity codeList on codeList.id = item.codeListId
            where item.codeListId = :codeListId
            order by item.id
            """)
    List<CodeListItemSnapshot> findSnapshotsByCodeListId(@Param("codeListId") Integer codeListId);

    /** Returns whether a row other than {@code excludedId} has {@code value} in the 1-based cell {@code index}. */
    @Query(value = """
            select exists(select 1
                          from code_list_items
                          where code_list_id = :codeListId
                            and columns[:index] = :value
                            and id <> :excludedId)
            """, nativeQuery = true)
    boolean existsByKeyValue(@Param("codeListId") Integer codeListId,
                             @Param("index") int index,
                             @Param("value") String value,
                             @Param("excludedId") long excludedId);

    @Transactional
    void deleteAllByCodeListId(Integer codeListId);
}
