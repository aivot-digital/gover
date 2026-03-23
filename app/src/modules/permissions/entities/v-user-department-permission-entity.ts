export interface VUserDepartmentPermissionEntity {
    userId: string;
    departmentId: number;
    systemRolePermissions: string[];
    systemRoleNames: string[];
    systemRoleIds: number[];
    domainRolePermissions: string[];
    domainRoleNames: string[];
    domainRoleIds: number[];
    permissions: string[];
}
