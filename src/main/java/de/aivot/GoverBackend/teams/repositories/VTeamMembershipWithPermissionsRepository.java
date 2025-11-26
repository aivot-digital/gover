package de.aivot.GoverBackend.teams.repositories;

import de.aivot.GoverBackend.teams.entities.VTeamMembershipWithPermissionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VTeamMembershipWithPermissionsRepository extends JpaRepository<VTeamMembershipWithPermissionsEntity, Integer>, JpaSpecificationExecutor<VTeamMembershipWithPermissionsEntity> {
}
