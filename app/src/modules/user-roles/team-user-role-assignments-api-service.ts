import {UserRoleAssignmentRequestDTO} from './dtos/user-role-assignment-request-dto';
import {OrgUserRoleAssignmentResponseDTO} from './dtos/org-user-role-assignment-response-dto';
import {BaseCrudApiService} from '../../services/base-crud-api-service';
import {TeamUserRoleAssignmentResponseDTO} from './dtos/team-user-role-assignment-response-dto';

interface TeamUserRoleAssignmentFilter {
    userRoleId: number;
    teamMembershipId: number;
    teamMembershipTeamId: number;
    teamMembershipUserId: string;
    teamMembershipTeamName: string;
    teamMembershipUserFullName: string;
}

export class TeamUserRoleAssignmentsApiService extends BaseCrudApiService<UserRoleAssignmentRequestDTO, TeamUserRoleAssignmentResponseDTO, TeamUserRoleAssignmentResponseDTO, UserRoleAssignmentRequestDTO, number, TeamUserRoleAssignmentFilter> {
    constructor() {
        super('/api/team-user-role-assignments/');
    }

    public initialize(): TeamUserRoleAssignmentResponseDTO {
        return {
            teamMembershipId: 0,
            teamMembershipTeamId: 0,
            teamMembershipTeamName: '',
            teamMembershipUserDeletedInIdp: false,
            teamMembershipUserEmail: '',
            teamMembershipUserEnabled: false,
            teamMembershipUserFirstName: '',
            teamMembershipUserFullName: '',
            teamMembershipUserGlobalAdmin: false,
            teamMembershipUserId: '',
            teamMembershipUserLastName: '',
            teamMembershipUserVerified: false,
            userRoleAssignmentId: 0,
            userRoleFormPermissionAnnotate: false,
            userRoleFormPermissionCreate: false,
            userRoleFormPermissionDelete: false,
            userRoleFormPermissionEdit: false,
            userRoleFormPermissionPublish: false,
            userRoleFormPermissionRead: false,
            userRoleId: 0,
            userRoleName: '',
            userRoleOrgUnitMemberPermissionEdit: false,
            userRoleProcessInstancePermissionAnnotate: false,
            userRoleProcessInstancePermissionCreate: false,
            userRoleProcessInstancePermissionDelete: false,
            userRoleProcessInstancePermissionEdit: false,
            userRoleProcessInstancePermissionRead: false,
            userRoleProcessPermissionAnnotate: false,
            userRoleProcessPermissionCreate: false,
            userRoleProcessPermissionDelete: false,
            userRoleProcessPermissionEdit: false,
            userRoleProcessPermissionPublish: false,
            userRoleProcessPermissionRead: false,
            userRoleTeamMemberPermissionEdit: false

        };
    }
}