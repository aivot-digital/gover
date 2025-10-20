package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.form.entities.FormWithMembershipEntity;
import de.aivot.GoverBackend.form.entities.FormWithMembershipEntityId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FormWithMembershipRepository extends ReadOnlyRepository<FormWithMembershipEntity, FormWithMembershipEntityId>, JpaSpecificationExecutor<FormWithMembershipEntity> {

}
