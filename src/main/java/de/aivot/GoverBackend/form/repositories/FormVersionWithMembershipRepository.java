package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.form.entities.FormVersionWithMembershipEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithMembershipEntityId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FormVersionWithMembershipRepository extends ReadOnlyRepository<FormVersionWithMembershipEntity, FormVersionWithMembershipEntityId>, JpaSpecificationExecutor<FormVersionWithMembershipEntity> {

}
