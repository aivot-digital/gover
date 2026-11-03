package de.aivot.prosuna.backend.codeLists.repositories;

import de.aivot.prosuna.backend.codeLists.entities.CodeListItemEntity;
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
