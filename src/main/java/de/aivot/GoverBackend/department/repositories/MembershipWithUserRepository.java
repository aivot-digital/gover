package de.aivot.GoverBackend.department.repositories;

import de.aivot.GoverBackend.department.entities.MembershipWithUserEntity;
import de.aivot.GoverBackend.department.entities.MembershipWithUserEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MembershipWithUserRepository extends JpaRepository<MembershipWithUserEntity, MembershipWithUserEntityId>, JpaSpecificationExecutor<MembershipWithUserEntity> {

}
