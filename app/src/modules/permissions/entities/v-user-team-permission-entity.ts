export interface VUserTeamPermissionEntity {
    userId: string;
    teamId: number;
    systemRolePermissions: string[];
    systemRoleNames: string[];
    systemRoleIds: number[];
    domainRolePermissions: string[];
    domainRoleNames: string[];
    domainRoleIds: number[];
    permissions: string[];
}
