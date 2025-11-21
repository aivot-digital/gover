package de.aivot.GoverBackend.department.repositories;

import de.aivot.GoverBackend.department.entities.OrganizationalUnitMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrganizationalUnitMembershipRepository extends JpaRepository<OrganizationalUnitMembershipEntity, Integer>, JpaSpecificationExecutor<OrganizationalUnitMembershipEntity> {

}
