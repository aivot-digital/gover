package de.aivot.gover.backend.form.repositories;

import de.aivot.gover.backend.form.entities.FormVersionEntity;
import de.aivot.gover.backend.form.entities.FormVersionEntityId;
import de.aivot.gover.backend.form.enums.FormStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FormVersionRepository extends JpaRepository<FormVersionEntity, FormVersionEntityId>, JpaSpecificationExecutor<FormVersionEntity> {
    @Query(value = """
            SELECT max(version) from form_versions where form_id = :formId;
            """, nativeQuery = true)
    Optional<Integer> maxVersionForFormId(@Param("formId") Integer formId);

    boolean existsByFormIdAndStatus(Integer id, FormStatus formStatus);
}
