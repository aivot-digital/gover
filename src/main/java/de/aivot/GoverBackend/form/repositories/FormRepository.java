package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.entities.Form;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;

public interface FormRepository extends JpaRepository<Form, Integer>, JpaSpecificationExecutor<Form> {
    @Query(value = """
            SELECT version
            FROM forms
            WHERE slug = ?1 AND status = ?2
            ORDER BY CAST(string_to_array(version, '.') AS int[]) DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> getLatestVersionBySlugAndStatus(String slug, FormStatus status);

    boolean existsBySlugAndVersion(String slug, String version);

    boolean existsByThemeId(Integer theme);

    boolean existsByStatusAndBundIdEnabledIsTrue(FormStatus status);

    boolean existsByStatusAndBayernIdEnabledIsTrue(FormStatus status);

    boolean existsByStatusAndShIdEnabledIsTrue(FormStatus status);

    boolean existsByStatusAndMukEnabledIsTrue(FormStatus status);

    Collection<Form> findAllByStatusNotIn(Collection<FormStatus> status);

    @Query(value = """
            SELECT exists(
                SELECT 1 FROM (
                    SELECT jsonb_array_elements(fms.identity_providers) ->> 'identityProviderKey' AS identity_provider_key
                    FROM forms AS fms
                ) AS links
                WHERE links.identity_provider_key = ?1
            );
            """, nativeQuery = true)
    boolean existsWithLinkedIdentityProvider(String identityProviderKey);
}
