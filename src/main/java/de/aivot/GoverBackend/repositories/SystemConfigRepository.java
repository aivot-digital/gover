package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.enums.SystemConfigKey;
import de.aivot.GoverBackend.models.entities.Application;
import de.aivot.GoverBackend.models.entities.SystemConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Collection;

@RepositoryRestResource(collectionResourceRel = "systemConfigs", path = "system-configs")
public interface SystemConfigRepository extends PagingAndSortingRepository<SystemConfig, SystemConfigKey>, CrudRepository<SystemConfig, SystemConfigKey> {
    @Query("SELECT conf FROM SystemConfig conf WHERE conf.isPublic = TRUE")
    Collection<SystemConfig> findPublicSystemConfigs();
}
