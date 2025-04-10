import {CrudApiService} from '../../services/crud-api-service';
import {DepartmentMembership} from './models/department-membership';
import {UserRole} from '../../data/user-role';
import {Api} from '../../hooks/use-api';
import {DepartmentMembershipResponseDTO} from './dtos/department-membership-response-dto';
import {DepartmentMembershipRequestDTO} from './dtos/department-membership-request-dto';

export interface DepartmentMembershipFilters {
    userId: string;
    departmentId: number;
    departmentIds: number[];
    membershipId: number;
    departmentName: string;
    userEmail: string;
    userName: string;
    userEnabled: boolean;
    userDeletedInIdp: boolean;
    userVerified: boolean;
    userGlobalAdmin: boolean;
    membershipRole: string;
    ignoreMemberships: boolean;
    deletedInIdp: boolean;
}

export class DepartmentMembershipsApiService extends CrudApiService<DepartmentMembershipRequestDTO, DepartmentMembershipResponseDTO, DepartmentMembershipResponseDTO, DepartmentMembershipResponseDTO, DepartmentMembershipResponseDTO, number, DepartmentMembershipFilters> {
    public constructor(api: Api) {
        super(api, 'department-memberships/');
    }

    public initialize(): DepartmentMembershipResponseDTO {
        return {
            id: 0,
            departmentId: 0,
            departmentName: '',
            userId: '',
            role: UserRole.Editor,
            userEmail: '',
            userFirstName: '',
            userLastName: '',
            userFullName: '',
            userEnabled: false,
            userDeletedInIdp: true,
            userGlobalAdmin: false,
            userVerified: false,
        };
    }
}