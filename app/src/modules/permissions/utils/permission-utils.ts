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
