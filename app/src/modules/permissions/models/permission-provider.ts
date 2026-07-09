export interface PermissionProvider {
    contextLabel: string;
    permissions: PermissionEntry[];
    supportsDomainRoleAssignment: boolean;
}

export interface PermissionEntry {
    permission: string;
    label: string;
    description: string;
}
