import {UserRole} from '../../../data/user-role';
import {User} from '../../users/models/user';

export interface DepartmentMembershipResponseDTO {
    id: number;
    departmentId: number;
    departmentName: string;
    userId: string;
    role: UserRole;
    userFirstName: string;
    userLastName: string;
    userFullName: string;
    userEmail: string;
    userEnabled: boolean;
    userDeletedInIdp: boolean;
    userVerified: boolean;
    userGlobalAdmin: boolean;
}

export function departmentMembershipResponseDTOasUser(membership: DepartmentMembershipResponseDTO): User {
    return {
        id: membership.userId,
        firstName: membership.userFirstName,
        lastName: membership.userLastName,
        fullName: membership.userFullName,
        enabled: membership.userEnabled,
        deletedInIdp: membership.userDeletedInIdp,
        globalAdmin: membership.userGlobalAdmin,
        verified: membership.userVerified,
        email: membership.userEmail,
    }
}


