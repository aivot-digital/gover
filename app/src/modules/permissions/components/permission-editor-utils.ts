import Fuse from 'fuse.js';
import {type PermissionEntry, type PermissionProvider} from '../models/permission-provider';

export type CrudPermissionType = 'create' | 'read' | 'update' | 'delete';

export type PermissionGroup = Pick<PermissionProvider, 'contextLabel' | 'permissions'> & {
    availabilityWarningLabel?: string;
    assignmentHint?: string | null;
};

export interface PermissionMeta {
    label?: string;
    description?: string;
}

interface IndexedPermission {
    groupLabel: string;
    permission: PermissionEntry;
    searchLabel: string;
    searchPermission: string;
    searchDescription: string;
    searchGroupLabel: string;
}

export function groupKey(label: string): string {
    return label.trim();
}

export function normalizeSearch(value: string): string {
    return value
        .trim()
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '');
}

export function toSortedPermissionList(permissions: Iterable<string>): string[] {
    return Array.from(new Set(permissions))
        .filter(Boolean)
        .sort();
}

export function buildAssignablePermissionGroups(
    apiPermissions: PermissionProvider[],
    onlyDomainRoleAssignable: boolean,
): PermissionGroup[] {
    if (!onlyDomainRoleAssignable) {
        return apiPermissions;
    }

    // Domain roles inherit provider-level assignability, then subtract per-permission exclusions.
    return apiPermissions
        .filter((group) => group.supportsDomainRoleAssignment)
        .map((group) => {
            const excludedPermissions = new Set(group.excludedFromDomainRoleAssignment ?? []);

            return {
                ...group,
                permissions: group.permissions.filter((permission) => !excludedPermissions.has(permission.permission)),
                assignmentHint: group.domainRoleAssignmentHint,
            };
        })
        .filter((group) => group.permissions.length > 0);
}

export function buildPermissionMetaMap(apiPermissions: PermissionProvider[]): Map<string, PermissionMeta> {
    const map = new Map<string, PermissionMeta>();

    for (const group of apiPermissions) {
        for (const permission of group.permissions) {
            map.set(permission.permission, {
                label: permission.label,
                description: permission.description,
            });
        }
    }

    return map;
}

export function buildPermissionSet(groups: PermissionGroup[]): Set<string> {
    const set = new Set<string>();

    for (const group of groups) {
        for (const permission of group.permissions) {
            set.add(permission.permission);
        }
    }

    return set;
}

export function buildRemovedFromSystemPermissions(
    selectedPermissions: string[],
    allApiPermissionSet: ReadonlySet<string>,
    hasLoadedApiPermissions: boolean,
): PermissionEntry[] {
    if (!hasLoadedApiPermissions) {
        return [];
    }

    return selectedPermissions
        .filter((permission) => !allApiPermissionSet.has(permission))
        .sort()
        .map((permission) => ({
            permission,
            label: permission,
            description: 'Diese Berechtigung ist im System nicht mehr vorhanden.',
        }));
}

export function buildUnavailableForDomainRolePermissions(
    selectedPermissions: string[],
    allApiPermissionSet: ReadonlySet<string>,
    assignablePermissionSet: ReadonlySet<string>,
    allApiPermissionMeta: ReadonlyMap<string, PermissionMeta>,
    onlyDomainRoleAssignable: boolean,
    hasLoadedApiPermissions: boolean,
): PermissionEntry[] {
    if (!onlyDomainRoleAssignable || !hasLoadedApiPermissions) {
        return [];
    }

    return selectedPermissions
        .filter((permission) => allApiPermissionSet.has(permission) && !assignablePermissionSet.has(permission))
        .sort()
        .map((permission) => {
            const meta = allApiPermissionMeta.get(permission);

            return {
                permission,
                label: meta?.label ?? permission,
                description: meta?.description ?? 'Diese Berechtigung ist für Domänenrollen nicht verfügbar.',
            };
        });
}

export function buildRecoveryPermissionGroups(
    removedFromSystemPermissions: PermissionEntry[],
    unavailableForDomainRolePermissions: PermissionEntry[],
): PermissionGroup[] {
    const recoveryGroups: PermissionGroup[] = [];

    if (removedFromSystemPermissions.length > 0) {
        recoveryGroups.push({
            contextLabel: 'Nicht mehr im System vorhandene Berechtigungen',
            permissions: removedFromSystemPermissions,
            availabilityWarningLabel: 'Nicht mehr vorhanden',
        });
    }

    if (unavailableForDomainRolePermissions.length > 0) {
        recoveryGroups.push({
            contextLabel: 'Für Domänenrollen nicht verfügbare Berechtigungen',
            permissions: unavailableForDomainRolePermissions,
            availabilityWarningLabel: 'Nicht verfügbar',
        });
    }

    return recoveryGroups;
}

export function buildPermissionSearchIndex(groups: PermissionGroup[]): Fuse<IndexedPermission> {
    const indexedPermissions: IndexedPermission[] = groups.flatMap((group) =>
        group.permissions.map((permission) => ({
            groupLabel: group.contextLabel,
            permission,
            searchLabel: normalizeSearch(permission.label),
            searchPermission: normalizeSearch(permission.permission),
            searchDescription: normalizeSearch(permission.description ?? ''),
            searchGroupLabel: normalizeSearch(group.contextLabel),
        })),
    );

    return new Fuse(indexedPermissions, {
        keys: ['searchLabel', 'searchPermission', 'searchDescription', 'searchGroupLabel'],
        threshold: 0.35,
        ignoreLocation: true,
    });
}

export function filterPermissionGroups(
    groups: PermissionGroup[],
    searchIndex: Fuse<IndexedPermission>,
    normalizedQuery: string,
): PermissionGroup[] {
    if (!normalizedQuery) {
        return groups;
    }

    const matchesByGroup = new Map<string, Set<string>>();
    const seen = new Set<string>();

    searchIndex.search(normalizedQuery).forEach(({item}) => {
        const key = `${groupKey(item.groupLabel)}::${item.permission.permission}`;
        if (seen.has(key)) {
            return;
        }
        seen.add(key);

        const existing = matchesByGroup.get(item.groupLabel) ?? new Set<string>();
        existing.add(item.permission.permission);
        matchesByGroup.set(item.groupLabel, existing);
    });

    return groups
        .map((group) => {
            const matches = matchesByGroup.get(group.contextLabel);

            return {
                ...group,
                // Permission providers define the intentional display order within a group.
                permissions: matches == null
                    ? []
                    : group.permissions.filter((permission) => matches.has(permission.permission)),
            };
        })
        .filter((group) => group.permissions.length > 0);
}

export function getPermissionKeys(groups: PermissionGroup[]): string[] {
    return toSortedPermissionList(groups.flatMap((group) => group.permissions.map((permission) => permission.permission)));
}

export function getSelectedGroupKeys(
    groups: PermissionGroup[],
    selectedPermissionsSet: ReadonlySet<string>,
): string[] {
    const keys: string[] = [];

    for (const group of groups) {
        if (group.permissions.some((permission) => selectedPermissionsSet.has(permission.permission))) {
            keys.push(groupKey(group.contextLabel));
        }
    }

    return keys;
}

export function buildPermissionDiff(originalPermissions: string[], selectedPermissions: string[]) {
    const original = new Set(originalPermissions ?? []);
    const current = new Set(selectedPermissions);

    const added: string[] = [];
    const removed: string[] = [];

    current.forEach((permission) => {
        if (!original.has(permission)) {
            added.push(permission);
        }
    });

    original.forEach((permission) => {
        if (!current.has(permission)) {
            removed.push(permission);
        }
    });

    added.sort();
    removed.sort();

    return {
        added,
        removed,
        hasChanges: added.length > 0 || removed.length > 0,
    };
}

export function inferCrud(permission: string): CrudPermissionType | null {
    const normalizedPermission = permission.toLowerCase();

    if (normalizedPermission.includes('create') || normalizedPermission.includes('add') || normalizedPermission.includes('new')) {
        return 'create';
    }
    if (normalizedPermission.includes('read') || normalizedPermission.includes('view') || normalizedPermission.includes('list') || normalizedPermission.includes('get')) {
        return 'read';
    }
    if (normalizedPermission.includes('update') || normalizedPermission.includes('edit') || normalizedPermission.includes('write')) {
        return 'update';
    }
    if (normalizedPermission.includes('delete') || normalizedPermission.includes('destroy') || normalizedPermission.includes('remove')) {
        return 'delete';
    }

    return null;
}
