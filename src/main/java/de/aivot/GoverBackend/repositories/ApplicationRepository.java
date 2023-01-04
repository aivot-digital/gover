package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.Application;
import javax.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "applications", path = "applications")
public interface ApplicationRepository extends PagingAndSortingRepository<Application, Long>, CrudRepository<Application, Long> {
    @Transactional
    Optional<Application> getBySlugAndVersion(String slug, String version);
}
