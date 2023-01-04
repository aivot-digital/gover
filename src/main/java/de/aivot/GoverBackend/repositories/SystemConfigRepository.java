package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.enums.SystemConfigKey;
import de.aivot.GoverBackend.models.SystemConfig;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "systemConfigs", path = "system-configs")
public interface SystemConfigRepository extends PagingAndSortingRepository<SystemConfig, SystemConfigKey> {
}