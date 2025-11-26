package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.form.entities.FormWithMembershipEntityId;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VFormWithPermissionRepository extends ReadOnlyRepository<VFormWithPermissionEntity, FormWithMembershipEntityId>, JpaSpecificationExecutor<VFormWithPermissionEntity> {

}
