import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {VTeamUserRoleAssignmentWithDetailsEntity} from "../entities/v-team-user-role-assignment-with-details-entity";
import {VTeamMembershipWithDetailsApiService} from "./v-team-membership-with-details-api-service";
import {UserRoleAssignmentEntity} from "../../user-roles/entities/user-role-assignment-entity";

interface VTeamUserRoleAssignmentWithDetailsFilter {
    id: number;
    teamId: number;
    name: string;
    userId: string;
    fullName: string;
    userRoleId: string;
}

export class VTeamUserRoleAssignmentWithDetailsApiService extends BaseCrudApiService<
    UserRoleAssignmentEntity,
    VTeamUserRoleAssignmentWithDetailsEntity,
    VTeamUserRoleAssignmentWithDetailsEntity,
    VTeamUserRoleAssignmentWithDetailsEntity,
    number,
    VTeamUserRoleAssignmentWithDetailsFilter
> {

    constructor() {
        super('/api/team-user-role-assignments-with-details/');
    }

    public initialize(): VTeamUserRoleAssignmentWithDetailsEntity {
        return VTeamUserRoleAssignmentWithDetailsApiService.initialize();
    }

    public static initialize(): VTeamUserRoleAssignmentWithDetailsEntity {
        return {
            ...VTeamMembershipWithDetailsApiService.initialize(),
            description: '',
            userRoleAssignmentId: 0,
            userRoleId: 0,
            userRoleName: '',
            departmentPermissionEdit: false,
            formPermissionAnnotate: false,
            formPermissionCreate: false,
            formPermissionDelete: false,
            formPermissionEdit: false,
            formPermissionPublish: false,
            formPermissionRead: false,
            processInstancePermissionAnnotate: false,
            processInstancePermissionCreate: false,
            processInstancePermissionDelete: false,
            processInstancePermissionEdit: false,
            processInstancePermissionRead: false,
            processPermissionAnnotate: false,
            processPermissionCreate: false,
            processPermissionDelete: false,
            processPermissionEdit: false,
            processPermissionPublish: false,
            processPermissionRead: false,
            teamPermissionEdit: false,
        };
    }
}