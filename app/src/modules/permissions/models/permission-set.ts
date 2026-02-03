import {type VUserDepartmentPermissionEntity} from '../entities/v-user-department-permission-entity';
import {type VUserTeamPermissionEntity} from '../entities/v-user-team-permission-entity';
import {type VUserDomainPermissionEntity} from '../entities/v-user-domain-permission-entity';
import {type VUserSystemPermissionEntity} from '../entities/v-user-system-permission-entity';

export interface PermissionSet {
    departmentPermissions: VUserDepartmentPermissionEntity[];
    teamPermissions: VUserTeamPermissionEntity[];
    domainPermissions: VUserDomainPermissionEntity[];
    systemPermissions: VUserSystemPermissionEntity[];
}
