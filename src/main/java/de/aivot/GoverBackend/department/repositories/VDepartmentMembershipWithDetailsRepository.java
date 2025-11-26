package de.aivot.GoverBackend.department.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.department.entities.VDepartmentMembershipWithDetailsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface VDepartmentMembershipWithDetailsRepository extends ReadOnlyRepository<VDepartmentMembershipWithDetailsEntity, Integer>, JpaSpecificationExecutor<VDepartmentMembershipWithDetailsEntity> {
}
