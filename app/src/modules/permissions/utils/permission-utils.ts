import {type Permission} from '../../../data/permissions/permission';
import {type PermissionSet} from '../models/permission-set';

export type PermissionLike = Permission | string;

export interface PermissionDeniedRouteError {
    status: 403;
    message: string;
}

export function createPermissionDeniedError(permission: PermissionLike): PermissionDeniedRouteError {
    return {
        status: 403,
        message: `Die Berechtigung ${permission} ist erforderlich.`,
    };
}

export function formatMissingPermissionTooltip(permission: PermissionLike): string {
    return `Sie besitzen nicht die erforderliche Berechtigung (${permission}).`;
}

export type PermissionScope<ItemType> =
    | { type: 'system' }
    | { type: 'team'; getResourceId: (item: ItemType) => number | undefined }
    | { type: 'department'; getResourceId: (item: ItemType) => number | undefined }
    | { type: 'process'; getResourceId: (item: ItemType) => number | undefined }
    | { type: 'processInstance'; getResourceId: (item: ItemType) => number | undefined }
    | { type: 'anyTeam' }
    | { type: 'anyDepartment' };

export type PermissionRequirement<ItemType> =
    | PermissionLike
    | {
        permission: PermissionLike;
        scope?: PermissionScope<ItemType>;
    };

// Frontend checks mirror backend semantics: a matching system permission grants the action globally.
export function hasSystemPermission(permissionSet: PermissionSet | undefined, permission: PermissionLike): boolean {
    return permissionSet?.systemPermissions
        ?.some((entry) => entry != null && entry.permissions?.includes(permission)) ?? false;
}

// The require* helpers throw route errors so StaffShell can render the existing access-denied handling.
export function requireSystemPermission(permissionSet: PermissionSet | undefined, permission: PermissionLike): void {
    if (!hasSystemPermission(permissionSet, permission)) {
        throw createPermissionDeniedError(permission);
    }
}

export function hasDepartmentPermission(
    permissionSet: PermissionSet | undefined,
    departmentId: number | undefined,
    permission: PermissionLike,
): boolean {
    return hasSystemPermission(permissionSet, permission) ||
        (
            departmentId != null &&
            (permissionSet?.departmentPermissions
                ?.some((entry) => entry != null && entry.departmentId === departmentId && entry.permissions?.includes(permission)) ?? false)
        );
}

export function requireDepartmentPermission(
    permissionSet: PermissionSet | undefined,
    departmentId: number | undefined,
    permission: PermissionLike,
): void {
    if (!hasDepartmentPermission(permissionSet, departmentId, permission)) {
        throw createPermissionDeniedError(permission);
    }
}

export function hasAnyDepartmentPermission(permissionSet: PermissionSet | undefined, permission: PermissionLike): boolean {
    return hasSystemPermission(permissionSet, permission) ||
        (
            permissionSet?.departmentPermissions
                ?.some((entry) => entry != null && entry.permissions?.includes(permission)) ?? false
        );
}

export function requireAnyDepartmentPermission(permissionSet: PermissionSet | undefined, permission: PermissionLike): void {
    if (!hasAnyDepartmentPermission(permissionSet, permission)) {
        throw createPermissionDeniedError(permission);
    }
}

export function hasTeamPermission(
    permissionSet: PermissionSet | undefined,
    teamId: number | undefined,
    permission: PermissionLike,
): boolean {
    return hasSystemPermission(permissionSet, permission) ||
        (
            teamId != null &&
            (permissionSet?.teamPermissions
                ?.some((entry) => entry != null && entry.teamId === teamId && entry.permissions?.includes(permission)) ?? false)
        );
}

export function requireTeamPermission(
    permissionSet: PermissionSet | undefined,
    teamId: number | undefined,
    permission: PermissionLike,
): void {
    if (!hasTeamPermission(permissionSet, teamId, permission)) {
        throw createPermissionDeniedError(permission);
    }
}

export function hasAnyTeamPermission(permissionSet: PermissionSet | undefined, permission: PermissionLike): boolean {
    return hasSystemPermission(permissionSet, permission) ||
        (
            permissionSet?.teamPermissions
                ?.some((entry) => entry != null && entry.permissions?.includes(permission)) ?? false
        );
}

export function requireAnyTeamPermission(permissionSet: PermissionSet | undefined, permission: PermissionLike): void {
    if (!hasAnyTeamPermission(permissionSet, permission)) {
        throw createPermissionDeniedError(permission);
    }
}

export function hasProcessPermission(
    permissionSet: PermissionSet | undefined,
    processId: number | undefined,
    permission: PermissionLike,
): boolean {
    return hasSystemPermission(permissionSet, permission) ||
        (
            processId != null &&
            (permissionSet?.processPermissions
                ?.some((entry) => entry != null && entry.processId === processId && entry.permissions?.includes(permission)) ?? false)
        );
}

export function requireProcessPermission(
    permissionSet: PermissionSet | undefined,
    processId: number | undefined,
    permission: PermissionLike,
): void {
    if (!hasProcessPermission(permissionSet, processId, permission)) {
        throw createPermissionDeniedError(permission);
    }
}

export function hasProcessInstancePermission(
    permissionSet: PermissionSet | undefined,
    processInstanceId: number | undefined,
    permission: PermissionLike,
): boolean {
    return hasSystemPermission(permissionSet, permission) ||
        (
            processInstanceId != null &&
            (permissionSet?.processInstancePermissions
                ?.some((entry) => entry != null && entry.processInstanceId === processInstanceId && entry.permissions?.includes(permission)) ?? false)
        );
}

export function requireProcessInstancePermission(
    permissionSet: PermissionSet | undefined,
    processInstanceId: number | undefined,
    permission: PermissionLike,
): void {
    if (!hasProcessInstancePermission(permissionSet, processInstanceId, permission)) {
        throw createPermissionDeniedError(permission);
    }
}

export function hasScopedPermission<ItemType>(
    permissionSet: PermissionSet | undefined,
    item: ItemType | undefined,
    scope: PermissionScope<ItemType>,
    permission: PermissionLike,
): boolean {
    // System permissions act as global grants for scoped resources as well. Keep this central so
    // generic page components cannot accidentally diverge from the backend permission semantics.
    switch (scope.type) {
        case 'system':
            return hasSystemPermission(permissionSet, permission);
        case 'anyDepartment':
            return hasAnyDepartmentPermission(permissionSet, permission);
        case 'anyTeam':
            return hasAnyTeamPermission(permissionSet, permission);
        case 'department':
            return item != null && hasDepartmentPermission(permissionSet, scope.getResourceId(item), permission);
        case 'team':
            return item != null && hasTeamPermission(permissionSet, scope.getResourceId(item), permission);
        case 'process':
            return item != null && hasProcessPermission(permissionSet, scope.getResourceId(item), permission);
        case 'processInstance':
            return item != null && hasProcessInstancePermission(permissionSet, scope.getResourceId(item), permission);
    }
}

export function resolvePermissionRequirement<ItemType>(
    requiredPermission: PermissionRequirement<ItemType> | undefined,
    defaultScope: PermissionScope<ItemType> | undefined,
): { permission: PermissionLike; scope: PermissionScope<ItemType> } | undefined {
    if (requiredPermission == null) {
        return undefined;
    }

    if (typeof requiredPermission === 'object' && 'permission' in requiredPermission) {
        return {
            permission: requiredPermission.permission,
            scope: requiredPermission.scope ?? defaultScope ?? {type: 'system'},
        };
    }

    return {
        permission: requiredPermission,
        scope: defaultScope ?? {type: 'system'},
    };
}
