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
export function checkSystemPermission(permissionSet: PermissionSet | undefined, permission: PermissionLike): boolean {
    return permissionSet?.systemPermissions
        ?.some((entry) => entry != null && entry.permissions?.includes(permission)) ?? false;
}

// The has* helpers throw route errors so StaffShell can render the existing access-denied handling.
export function hasSystemPermission(permissionSet: PermissionSet | undefined, permission: PermissionLike): void {
    if (!checkSystemPermission(permissionSet, permission)) {
        throw createPermissionDeniedError(permission);
    }
}

export function checkDepartmentPermission(
    permissionSet: PermissionSet | undefined,
    departmentId: number | undefined,
    permission: PermissionLike,
): boolean {
    return checkSystemPermission(permissionSet, permission) ||
        (
            departmentId != null &&
            (permissionSet?.departmentPermissions
                ?.some((entry) => entry != null && entry.departmentId === departmentId && entry.permissions?.includes(permission)) ?? false)
        );
}

export function hasDepartmentPermission(
    permissionSet: PermissionSet | undefined,
    departmentId: number | undefined,
    permission: PermissionLike,
): void {
    if (!checkDepartmentPermission(permissionSet, departmentId, permission)) {
        throw createPermissionDeniedError(permission);
    }
}

export function checkTeamPermission(
    permissionSet: PermissionSet | undefined,
    teamId: number | undefined,
    permission: PermissionLike,
): boolean {
    return checkSystemPermission(permissionSet, permission) ||
        (
            teamId != null &&
            (permissionSet?.teamPermissions
                ?.some((entry) => entry != null && entry.teamId === teamId && entry.permissions?.includes(permission)) ?? false)
        );
}

export function hasTeamPermission(
    permissionSet: PermissionSet | undefined,
    teamId: number | undefined,
    permission: PermissionLike,
): void {
    if (!checkTeamPermission(permissionSet, teamId, permission)) {
        throw createPermissionDeniedError(permission);
    }
}

export function checkProcessPermission(
    permissionSet: PermissionSet | undefined,
    processId: number | undefined,
    permission: PermissionLike,
): boolean {
    return checkSystemPermission(permissionSet, permission) ||
        (
            processId != null &&
            (permissionSet?.processPermissions
                ?.some((entry) => entry != null && entry.processId === processId && entry.permissions?.includes(permission)) ?? false)
        );
}

export function hasProcessPermission(
    permissionSet: PermissionSet | undefined,
    processId: number | undefined,
    permission: PermissionLike,
): void {
    if (!checkProcessPermission(permissionSet, processId, permission)) {
        throw createPermissionDeniedError(permission);
    }
}

export function checkProcessInstancePermission(
    permissionSet: PermissionSet | undefined,
    processInstanceId: number | undefined,
    permission: PermissionLike,
): boolean {
    return checkSystemPermission(permissionSet, permission) ||
        (
            processInstanceId != null &&
            (permissionSet?.processInstancePermissions
                ?.some((entry) => entry != null && entry.processInstanceId === processInstanceId && entry.permissions?.includes(permission)) ?? false)
        );
}

export function hasProcessInstancePermission(
    permissionSet: PermissionSet | undefined,
    processInstanceId: number | undefined,
    permission: PermissionLike,
): void {
    if (!checkProcessInstancePermission(permissionSet, processInstanceId, permission)) {
        throw createPermissionDeniedError(permission);
    }
}
