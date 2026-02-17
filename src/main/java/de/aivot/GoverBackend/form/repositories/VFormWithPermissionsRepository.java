package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntity;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntityId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VFormWithPermissionsRepository extends ReadOnlyRepository<VFormWithPermissionsEntity, VFormWithPermissionsEntityId>, JpaSpecificationExecutor<VFormWithPermissionsEntity> {

}
