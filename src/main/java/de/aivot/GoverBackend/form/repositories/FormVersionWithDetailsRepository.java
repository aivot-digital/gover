package de.aivot.GoverBackend.form.repositories;

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

import java.util.Optional;

public interface FormVersionWithDetailsRepository extends JpaRepository<FormVersionWithDetailsEntity, FormVersionWithDetailsEntityId>, JpaSpecificationExecutor<FormVersionWithDetailsEntity> {
    Page<FormVersionWithDetailsEntity> findAllByPublishedIsNotNullAndRevokedIsNull(Pageable pageable, Specification<FormVersionWithDetailsEntity> specification);

    @Query(value = """
            SELECT *
            FROM form_version_with_details
            WHERE slug = :slug
            ORDER BY version DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<FormVersionWithDetailsEntity> findLatestForSlug(@Param("slug") String slug);

    @Query(value = """
            SELECT *
            FROM form_version_with_details
            WHERE slug = :slug and state = :status
            ORDER BY version DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<FormVersionWithDetailsEntity> findLatestForSlugAndStatus(@Param("slug") String slug, @Param("status") FormStatus status);

    @Query(value = """
            SELECT *
            FROM form_version_with_details
            WHERE slug = :slug and version = :version
            LIMIT 1
            """, nativeQuery = true)
    Optional<FormVersionWithDetailsEntity> findVersionForSlugAndVersion(@Param("slug") String slug, @Param("version") Integer version);
}
