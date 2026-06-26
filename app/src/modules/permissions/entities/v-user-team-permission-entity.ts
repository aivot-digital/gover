export interface VUserTeamPermissionEntity {
    userId: string;
    teamId: number;
    isDirectMember?: boolean;
    isIndirectMember?: boolean;
    directSystemRolePermissions?: string[];
    indirectSystemRolePermissions?: string[];
    systemRolePermissions: string[];
    systemRoleNames: string[];
    systemRoleIds: number[];
    directDomainRolePermissions?: string[];
    indirectDomainRolePermissions?: string[];
    domainRolePermissions: string[];
    domainRoleNames: string[];
    domainRoleIds: number[];
    directPermissions?: string[];
    indirectPermissions?: string[];
    deputyForUserIds?: string[];
    permissions: string[];
}
