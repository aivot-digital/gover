package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.form.entities.FormWithMembership;
import de.aivot.GoverBackend.form.entities.FormWithMembershipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FormWithMembershipRepository extends JpaRepository<FormWithMembership, FormWithMembershipId>, JpaSpecificationExecutor<FormWithMembership> {

}
