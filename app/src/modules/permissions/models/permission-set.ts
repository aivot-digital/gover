import {type VUserDepartmentPermissionEntity} from '../entities/v-user-department-permission-entity';
import {type VUserTeamPermissionEntity} from '../entities/v-user-team-permission-entity';
import {type VUserSystemPermissionEntity} from '../entities/v-user-system-permission-entity';

export interface PermissionSet {
    departmentPermissions: VUserDepartmentPermissionEntity[];
    teamPermissions: VUserTeamPermissionEntity[];
    domainPermissions: DomainPermission[];
    processPermissions: ProcessPermission[];
    processInstancePermissions: ProcessInstancePermission[];
    systemPermissions: VUserSystemPermissionEntity[];
}

export interface DomainPermission {
    id: string;
    userId: string;
    departmentId?: number;
    teamId?: number;
    permissions: string[];
}

export interface ProcessPermission {
    id: string;
    userId: string;
    viaSourceTeamId?: number;
    viaSourceDepartmentId?: number;
    processId: number;
    permissions: string[];
}

export interface ProcessInstancePermission {
    id: string;
    userId: string;
    viaSourceTeamId?: number;
    viaSourceDepartmentId?: number;
    processInstanceId: number;
    permissions: string[];
}
