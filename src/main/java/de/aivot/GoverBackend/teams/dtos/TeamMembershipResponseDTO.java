package de.aivot.GoverBackend.teams.dtos;

import de.aivot.GoverBackend.teams.entities.TeamEntity;
import de.aivot.GoverBackend.teams.entities.TeamMembershipEntity;
import de.aivot.GoverBackend.teams.entities.VTeamMembershipWithDetailsEntity;
import de.aivot.GoverBackend.user.entities.UserEntity;

public record TeamMembershipResponseDTO(
        Integer id,

        Integer teamId,
        String teamName,

        String userId,
        String userFirstName,
        String userLastName,
        String userFullName,
        String userEmail,
        Boolean userEnabled,
        Boolean userVerified,
        Boolean userGlobalAdmin,
        Boolean userDeletedInIdp
) {
    public static TeamMembershipResponseDTO fromEntity(VTeamMembershipWithDetailsEntity entity) {
        return new TeamMembershipResponseDTO(
                entity.getId(),
                entity.getTeamId(),
                entity.getName(),
                entity.getUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getEnabled(),
                entity.getVerified(),
                UserEntity.SUPER_ADMIN_ROLE_VALUE.equals(entity.getGlobalRole()),
                entity.getDeletedInIdp()
        );
    }

    public static TeamMembershipResponseDTO fromEntity(TeamMembershipEntity membership, TeamEntity team, UserEntity user) {
        return new TeamMembershipResponseDTO(
                membership.getId(),

                team.getId(),
                team.getName(),

                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getEmail(),
                user.getEnabled(),
                user.getVerified(),
                user.getIsSuperAdmin(),
                user.getDeletedInIdp()
        );
    }
}

