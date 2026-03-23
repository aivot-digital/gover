export interface VUserDomainPermissionEntity {
    userId: string;
    departmentId?: number;
    teamId?: number;
    systemRolePermissions: string[];
    systemRoleNames: string[];
    systemRoleIds: number[];
    domainRolePermissions: string[];
    domainRoleNames: string[];
    domainRoleIds: number[];
    permissions: string[];
}
