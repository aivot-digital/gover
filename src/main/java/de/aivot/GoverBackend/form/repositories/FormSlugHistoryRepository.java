package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.form.entities.FormSlugHistoryEntity;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FormSlugHistoryRepository extends JpaRepository<FormSlugHistoryEntity, String>, JpaSpecificationExecutor<FormSlugHistoryEntity> {
    boolean existsBySlugAndFormIdIsNot(@Nonnull String slug, @Nonnull Integer formId);
}
