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
    Collection<Application> findAllByStatus(ApplicationStatus status);

    @Transactional(readOnly = true)
    Collection<Application> findAllByIdIn(List<Integer> ids);
}
