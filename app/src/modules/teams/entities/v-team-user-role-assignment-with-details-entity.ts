import {VTeamMembershipWithDetailsEntity} from "./v-team-membership-with-details-entity";

export interface VTeamUserRoleAssignmentWithDetailsEntity extends VTeamMembershipWithDetailsEntity {
    userRoleAssignmentId: number;
    userRoleId: number;
    userRoleName: string;
    description: string;
    departmentPermissionEdit: boolean;
    teamPermissionEdit: boolean;
    formPermissionCreate: boolean;
    formPermissionRead: boolean;
    formPermissionEdit: boolean;
    formPermissionDelete: boolean;
    formPermissionAnnotate: boolean;
    formPermissionPublish: boolean;
    processPermissionCreate: boolean;
    processPermissionRead: boolean;
    processPermissionEdit: boolean;
    processPermissionDelete: boolean;
    processPermissionAnnotate: boolean;
    processPermissionPublish: boolean;
    processInstancePermissionCreate: boolean;
    processInstancePermissionRead: boolean;
    processInstancePermissionEdit: boolean;
    processInstancePermissionDelete: boolean;
    processInstancePermissionAnnotate: boolean;
}