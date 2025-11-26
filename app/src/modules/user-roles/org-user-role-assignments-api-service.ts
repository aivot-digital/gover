import {UserRoleAssignmentRequestDTO} from './dtos/user-role-assignment-request-dto';
import {OrgUserRoleAssignmentResponseDTO} from './dtos/org-user-role-assignment-response-dto';
import {BaseCrudApiService} from '../../services/base-crud-api-service';

interface OrgUserRoleAssignmentFilter {
    userRoleId: number;
    orgUnitMembershipId: number;
    orgUnitMembershipOrganizationalUnitId: number;
    orgUnitMembershipUserId: string;
    orgUnitMembershipOrganizationalUnitName: string;
    orgUnitMembershipUserFullName: string;
}

export class OrgUserRoleAssignmentsApiService extends BaseCrudApiService<UserRoleAssignmentRequestDTO, OrgUserRoleAssignmentResponseDTO, OrgUserRoleAssignmentResponseDTO, UserRoleAssignmentRequestDTO, number, OrgUserRoleAssignmentFilter> {
    constructor() {
        super('/api/department-user-role-assignments-with-details/');
    }

    public initialize(): OrgUserRoleAssignmentResponseDTO {
        return {
            orgUnitMembershipId: 0,
            orgUnitMembershipOrganizationalUnitDepth: 0,
            orgUnitMembershipOrganizationalUnitId: 0,
            orgUnitMembershipOrganizationalUnitName: '',
            orgUnitMembershipUserDeletedInIdp: false,
            orgUnitMembershipUserEmail: '',
            orgUnitMembershipUserEnabled: false,
            orgUnitMembershipUserFirstName: '',
            orgUnitMembershipUserFullName: '',
            orgUnitMembershipUserGlobalAdmin: false,
            orgUnitMembershipUserId: '',
            orgUnitMembershipUserLastName: '',
            orgUnitMembershipUserVerified: false,
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
            userRoleTeamMemberPermissionEdit: false,
        };
    }
}