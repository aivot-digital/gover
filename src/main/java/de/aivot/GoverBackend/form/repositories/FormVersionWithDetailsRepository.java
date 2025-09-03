package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntityId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface FormVersionWithDetailsRepository extends ReadOnlyRepository<FormVersionWithDetailsEntity, FormVersionWithDetailsEntityId>, JpaSpecificationExecutor<FormVersionWithDetailsEntity> {
    @Nonnull
    Optional<FormVersionWithDetailsEntity> findBySlugAndVersion(@Nonnull String slug, @Nonnull Integer version);
}
