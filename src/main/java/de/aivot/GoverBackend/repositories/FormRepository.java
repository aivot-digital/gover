package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.enums.ApplicationStatus;
import de.aivot.GoverBackend.models.entities.Form;
import de.aivot.GoverBackend.models.entities.FormListProjection;
import de.aivot.GoverBackend.models.entities.FormListProjectionPublic;
import de.aivot.GoverBackend.models.entities.FormPublicProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FormRepository extends JpaRepository<Form, Integer> {

    Collection<FormListProjection> findAllByIdIn(List<Integer> id);


    Optional<FormPublicProjection> getBySlugAndVersion(String slug, String version);

    @Query(
            value = """
                    SELECT *
                    FROM forms
                    WHERE slug = ?1 AND status = ?2
                    ORDER BY CAST(string_to_array(version, '.') AS int[]) DESC
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<FormPublicProjection> getLatestBySlugAndStatus(String slug, ApplicationStatus status);

    boolean existsBySlugAndVersion(String slug, String version);


    boolean existsBySlug(String slug);


    boolean existsBySlugAndDevelopingDepartmentId(String slug, Integer department);


    boolean existsByDestinationId(Integer destination);


    boolean existsByThemeId(Integer theme);


    boolean existsByStatusAndBundIdEnabledIsTrue(ApplicationStatus status);


    boolean existsByStatusAndBayernIdEnabledIsTrue(ApplicationStatus status);


    boolean existsByStatusAndShIdEnabledIsTrue(ApplicationStatus status);


    boolean existsByStatusAndMukEnabledIsTrue(ApplicationStatus status);

    @Query(
            value = """
                                SELECT fms.id as id,
                                       fms.slug as slug,
                                       fms.version as version,
                                       fms.root->>'headline' as title,
                                       fms.legal_support_department_id as legalSupportDepartmentId,
                                       fms.technical_support_department_id as technicalSupportDepartmentId,
                                       fms.imprint_department_id as imprintDepartmentId,
                                       fms.privacy_department_id as privacyDepartmentId,
                                       fms.accessibility_department_id as accessibilityDepartmentId,
                                       fms.developing_department_id as developingDepartmentId,
                                       fms.managing_department_id as managingDepartmentId,
                                       fms.responsible_department_id as responsibleDepartmentId,
                                       fms.theme_id as themeId,
                                       fms.bund_id_enabled as bundIdEnabled,
                                       fms.bayern_id_enabled as bayernIdEnabled,
                                       fms.muk_enabled as mukEnabled,
                                       fms.sh_id_enabled as shIdEnabled,
                                       fms.updated as updated
                                FROM forms fms
                                WHERE fms.status = ?1
                                ORDER BY fms.updated DESC
                    """,
            nativeQuery = true
    )
    Collection<FormListProjectionPublic> findAllPublicByStatus(Integer status);

    Collection<FormListProjection> findAllByDevelopingDepartmentId(Integer department);

    Collection<FormListProjection> findAllByManagingDepartmentId(Integer department);

    Collection<FormListProjection> findAllByResponsibleDepartmentId(Integer department);

    Collection<FormListProjection> findAllByDestinationId(Integer destination);

    Collection<FormListProjection> findAllByThemeId(Integer destination);

    @Query(
            """
                                SELECT fms.id as id,
                                       fms.slug as slug,
                                       fms.version as version,
                                       fms.title as title,
                                       fms.status as status,
                                       fms.destinationId as destinationId,
                                       fms.legalSupportDepartmentId as legalSupportDepartmentId,
                                       fms.technicalSupportDepartmentId as technicalSupportDepartmentId,
                                       fms.imprintDepartmentId as imprintDepartmentId,
                                       fms.privacyDepartmentId as privacyDepartmentId,
                                       fms.accessibilityDepartmentId as accessibilityDepartmentId,
                                       fms.developingDepartmentId as developingDepartmentId,
                                       fms.managingDepartmentId as managingDepartmentId,
                                       fms.responsibleDepartmentId as responsibleDepartmentId,
                                       fms.themeId as themeId,
                                       fms.created as created,
                                       fms.updated as updated,
                                       fms.customerAccessHours as customerAccessHours,
                                       fms.submissionDeletionWeeks as submissionDeletionWeeks,
                                       fms.bundIdEnabled as bundIdEnabled,
                                       fms.bundIdLevel as bundIdLevel,
                                       fms.bayernIdEnabled as bayernIdEnabled,
                                       fms.bayernIdLevel as bayernIdLevel,
                                       fms.mukEnabled as mukEnabled,
                                       fms.mukLevel as mukLevel,
                                       fms.shIdEnabled as shIdEnabled,
                                       fms.shIdLevel as shIdLevel
                                FROM Form fms
                                         LEFT OUTER JOIN DepartmentMembership mems ON fms.developingDepartmentId = mems.departmentId
                                WHERE mems.userId = ?1
                    """
    )
    Collection<FormListProjection> findAllByDevelopingDepartmentMemberUserId(String userId);


    @Query("""
            SELECT DISTINCT
                fms.id
            FROM Form fms
                LEFT OUTER JOIN DepartmentMembership dms ON (fms.responsibleDepartmentId is null AND fms.managingDepartmentId is null AND fms.developingDepartmentId = dms.departmentId) OR fms.responsibleDepartmentId = dms.departmentId OR fms.managingDepartmentId = dms.departmentId
            WHERE dms.userId = ?1
            """)
    Collection<Integer> findAccessibleFormIds(String userId);


    Collection<Form> findAllByStatusNotIn(Collection<ApplicationStatus> status);

    boolean existsByDevelopingDepartmentIdOrManagingDepartmentIdOrResponsibleDepartmentId(Integer departmentId1, Integer departmentId2, Integer departmentId3);
}
