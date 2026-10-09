package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessTaskDeadlineContinuationEntity;
import jakarta.annotation.Nonnull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessTaskDeadlineContinuationRepository extends JpaRepository<ProcessTaskDeadlineContinuationEntity, Long> {
    @Nonnull
    List<ProcessTaskDeadlineContinuationEntity> findByIdGreaterThanOrderByIdAsc(@Nonnull Long afterId, @Nonnull Pageable page);
}
