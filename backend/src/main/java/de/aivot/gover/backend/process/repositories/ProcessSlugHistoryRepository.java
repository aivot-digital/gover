package de.aivot.gover.backend.process.repositories;

import de.aivot.gover.backend.process.entities.ProcessSlugHistoryEntity;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProcessSlugHistoryRepository extends JpaRepository<ProcessSlugHistoryEntity, String>, JpaSpecificationExecutor<ProcessSlugHistoryEntity> {
    boolean existsBySlugAndProcessIdIsNot(@Nonnull String slug, @Nonnull Integer processId);

    Optional<ProcessSlugHistoryEntity> findBySlug(@Nonnull String slug);

    List<ProcessSlugHistoryEntity> findAllByProcessIdOrderByCreatedDesc(@Nonnull Integer processId);

    void deleteAllByProcessId(@Nonnull Integer processId);
}
