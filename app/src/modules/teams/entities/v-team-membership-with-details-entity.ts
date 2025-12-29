import {UserRoleResponseDTO} from "../../user-roles/dtos/user-role-response-dto";
import {Permission} from "../../../data/permissions/permission";

export interface VTeamMembershipWithDetailsEntity {
    membershipId: number;

    membershipIsDeputy: boolean;
    membershipAsDeputyForUserId: string | null;
    membershipAsDeputyForUserEmail: string | null;
    membershipAsDeputyForUserFirstName: string | null;
    membershipAsDeputyForUserLastName: string | null;
    membershipAsDeputyForUserFullName: string | null;
    membershipAsDeputyForUserEnabled: boolean | null;
    membershipAsDeputyForUserVerified: boolean | null;
    membershipAsDeputyForUserDeletedInIdp: boolean | null;
    membershipAsDeputyForUserSystemRoleId: number | null;

    teamId: number;
    teamName: string;

    userId: string;
    userEmail: string | null;
    userFirstName: string | null;
    userLastName: string | null;
    userFullName: string | null;
    userEnabled: boolean;
    userVerified: boolean;
    userDeletedInIdp: boolean;
    userSystemRoleId: number;

    domainRoles: UserRoleResponseDTO[];
    domainRolePermissions: Permission[];
}
