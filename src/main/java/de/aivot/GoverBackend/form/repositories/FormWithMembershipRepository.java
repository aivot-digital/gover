package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.form.entities.FormVersionWithMembershipEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithMembershipEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FormWithMembershipRepository extends JpaRepository<FormVersionWithMembershipEntity, FormVersionWithMembershipEntityId>, JpaSpecificationExecutor<FormVersionWithMembershipEntity> {

}
