package de.aivot.gover.backend.form.repositories;

import de.aivot.gover.backend.core.repositories.ReadOnlyRepository;
import de.aivot.gover.backend.form.entities.VFormWithPermissionsEntity;
import de.aivot.gover.backend.form.entities.VFormWithPermissionsEntityId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VFormWithPermissionsRepository extends ReadOnlyRepository<VFormWithPermissionsEntity, VFormWithPermissionsEntityId>, JpaSpecificationExecutor<VFormWithPermissionsEntity> {

}
