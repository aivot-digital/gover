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
                entity.getMembershipId(),
                entity.getTeamId(),
                entity.getTeamName(),
                entity.getUserId(),
                entity.getUserFirstName(),
                entity.getUserLastName(),
                entity.getUserFullName(),
                entity.getUserEmail(),
                entity.getUserEnabled(),
                entity.getUserVerified(),
                entity.getUserGlobalAdmin(),
                entity.getUserDeletedInIdp()
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
                user.getGlobalAdmin(),
                user.getDeletedInIdp()
        );
    }
}

