import {UserRoleResponseDTO} from "../../user-roles/dtos/user-role-response-dto";
import {Permission} from "../../../data/permissions/permission";
import {KeysToSnakeCase} from "../../../utils/camel-to-snake";
import {User} from "../../users/models/user";

export interface VTeamMembershipWithDetailsEntity {
    membershipId: number;
    membershipHasDeputies: boolean;
    membershipDeputies: KeysToSnakeCase<User>[];
    userId: string;
    userEmail: string | null;
    userFirstName: string | null;
    userLastName: string | null;
    userFullName: string | null;
    userEnabled: boolean;
    userVerified: boolean;
    userDeletedInIdp: boolean;
    userSystemRoleId: number;
    teamId: number;
    teamName: string;
    domainRoles: UserRoleResponseDTO[];
    domainRolePermissions: Permission[];
}
