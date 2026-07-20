export interface PermissionProvider {
    contextLabel: string;
    permissions: PermissionEntry[];
    supportsDomainRoleAssignment: boolean;
    excludedFromDomainRoleAssignment: string[];
    domainRoleAssignmentHint?: string | null;
    systemRoleAssignmentHint?: string | null;
}

export interface PermissionEntry {
    permission: string;
    label: string;
    description: string;
}
