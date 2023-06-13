package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.Application;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "applications", path = "applications")
public interface ApplicationRepository extends PagingAndSortingRepository<Application, Long>, CrudRepository<Application, Long> {
    @Transactional(readOnly = true)
    Optional<Application> getBySlugAndVersion(String slug, String version);

    @Transactional(readOnly = true)
    @Query("SELECT app FROM Application app WHERE app.status = 2")
    Collection<Application> findPublishedApplications();
}
