package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.enums.ApplicationStatus;
import de.aivot.GoverBackend.models.entities.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    @Transactional(readOnly = true)
    Optional<Application> getBySlugAndVersion(String slug, String version);

    @Transactional(readOnly = true)
    boolean existsBySlugAndVersion(String slug, String version);

    @Transactional(readOnly = true)
    boolean existsBySlug(String slug);

    @Transactional(readOnly = true)
    boolean existsBySlugAndDevelopingDepartment_Id(String slug, Integer department);

    @Transactional(readOnly = true)
    boolean existsByDestination_Id(Integer destination);

    @Transactional(readOnly = true)
    boolean existsByTheme_Id(Integer theme);

    @Transactional(readOnly = true)
    Collection<Application> findAllByStatus(ApplicationStatus status);

    @Transactional(readOnly = true)
    Collection<Application> findAllByIdIn(List<Integer> ids);

    @Transactional(readOnly = true)
    Collection<Application> findAllByDevelopingDepartmentId(Integer department);

    @Transactional(readOnly = true)
    Collection<Application> findAllByDestinationId(Integer destination);
}
