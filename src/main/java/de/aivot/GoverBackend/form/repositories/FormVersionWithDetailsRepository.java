package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntityId;
import de.aivot.GoverBackend.form.enums.FormStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public interface FormVersionWithDetailsRepository extends ReadOnlyRepository<FormVersionWithDetailsEntity, FormVersionWithDetailsEntityId>, JpaSpecificationExecutor<FormVersionWithDetailsEntity> {
    @Nonnull
    Page<FormVersionWithDetailsEntity> findAllByIsCurrentlyPublishedVersionIsTrue(@Nullable Pageable pageable, @Nullable Specification<FormVersionWithDetailsEntity> specification);

    @Nonnull
    Optional<FormVersionWithDetailsEntity> findBySlugAndIsCurrentlyPublishedVersionIsTrue(@Nonnull String slug);

    @Nonnull
    Optional<FormVersionWithDetailsEntity> findBySlugAndVersion(@Nonnull String slug, @Nonnull Integer version);
}
