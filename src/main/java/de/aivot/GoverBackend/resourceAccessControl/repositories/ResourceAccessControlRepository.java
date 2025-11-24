package de.aivot.GoverBackend.resourceAccessControl.repositories;

import de.aivot.GoverBackend.resourceAccessControl.entities.ResourceAccessControlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceAccessControlRepository extends JpaRepository<ResourceAccessControlEntity, Integer>, JpaSpecificationExecutor<ResourceAccessControlEntity> {
}

