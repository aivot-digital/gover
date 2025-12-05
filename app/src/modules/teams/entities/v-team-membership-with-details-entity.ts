import {TeamMembershipEntity} from "./team-membership-entity";
import {SystemUserRole} from "../../users/models/user";
import {VTeamUserRoleAssignmentWithDetailsEntity} from "./v-team-user-role-assignment-with-details-entity";

export interface VTeamMembershipWithDetailsEntity extends TeamMembershipEntity {
    name: string;

    firstName: string | null;
    lastName: string | null;
    fullName: string | null;
    email: string | null;
    enabled: boolean;
    verified: boolean;
    globalRole: SystemUserRole;
    deletedInIdp: boolean;
}

export interface  VTeamMembershipWithDetailsEntityWithRoles extends VTeamMembershipWithDetailsEntity {
    roles: VTeamUserRoleAssignmentWithDetailsEntity[];
}