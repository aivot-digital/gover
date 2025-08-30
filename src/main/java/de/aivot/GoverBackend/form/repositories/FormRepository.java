package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.form.entities.FormEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FormRepository extends JpaRepository<FormEntity, Integer>, JpaSpecificationExecutor<FormEntity> {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdIsNot(String slug, Integer id);
}
