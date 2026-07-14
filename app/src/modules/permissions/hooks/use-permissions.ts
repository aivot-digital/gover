import {useCallback} from 'react';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectPermissions, selectUser, setPermissions} from '../../../slices/user-slice';
import {PermissionApiService} from '../permission-api-service';
import {broadcastCrossTabInvalidation} from '../../../hooks/use-cross-tab-invalidation';
import {
    checkAnyDepartmentPermission,
    checkAnyTeamPermission,
    checkDepartmentPermission,
    checkProcessInstancePermission,
    checkProcessPermission,
    checkSystemPermission,
    checkTeamPermission,
    hasAnyDepartmentPermission,
    hasAnyTeamPermission,
    hasDepartmentPermission,
    hasProcessInstancePermission,
    hasProcessPermission,
    hasSystemPermission,
    hasTeamPermission,
    type PermissionLike,
} from '../utils/permission-utils';

export const PERMISSION_SET_INVALIDATION_KEY = 'permissions';

interface RefreshPermissionSetOptions {
    broadcast?: boolean;
}

export function useRefreshPermissionSet() {
    const dispatch = useAppDispatch();
    const user = useAppSelector(selectUser);
    const userId = user?.id;

    return useCallback(async (options?: RefreshPermissionSetOptions) => {
        if (userId == null) {
            dispatch(setPermissions(undefined));
            return undefined;
        }

        // Refresh permissions explicitly after access-changing mutations; do not use focus polling, as that would keep sessions alive.
        const permissionSet = await new PermissionApiService()
            .getOwnPermissionSet();

        dispatch(setPermissions(permissionSet));

        if (options?.broadcast === true) {
            broadcastCrossTabInvalidation({
                key: PERMISSION_SET_INVALIDATION_KEY,
                scope: userId,
            });
        }

        return permissionSet;
    }, [dispatch, userId]);
}

export function useCheckSystemPermission(permission: PermissionLike): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return checkSystemPermission(permissionSet, permission);
}

export function useHasSystemPermission(permission: PermissionLike): void {
    const permissionSet = useAppSelector(selectPermissions);
    hasSystemPermission(permissionSet, permission);
}

export function useCheckDepartmentPermission(
    departmentId: number | undefined,
    permission: PermissionLike,
): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return checkDepartmentPermission(permissionSet, departmentId, permission);
}

export function useHasDepartmentPermission(
    departmentId: number | undefined,
    permission: PermissionLike,
): void {
    const permissionSet = useAppSelector(selectPermissions);
    hasDepartmentPermission(permissionSet, departmentId, permission);
}

export function useCheckAnyDepartmentPermission(permission: PermissionLike): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return checkAnyDepartmentPermission(permissionSet, permission);
}

export function useHasAnyDepartmentPermission(permission: PermissionLike): void {
    const permissionSet = useAppSelector(selectPermissions);
    hasAnyDepartmentPermission(permissionSet, permission);
}

export function useCheckTeamPermission(
    teamId: number | undefined,
    permission: PermissionLike,
): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return checkTeamPermission(permissionSet, teamId, permission);
}

export function useHasTeamPermission(
    teamId: number | undefined,
    permission: PermissionLike,
): void {
    const permissionSet = useAppSelector(selectPermissions);
    hasTeamPermission(permissionSet, teamId, permission);
}

export function useCheckAnyTeamPermission(permission: PermissionLike): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return checkAnyTeamPermission(permissionSet, permission);
}

export function useHasAnyTeamPermission(permission: PermissionLike): void {
    const permissionSet = useAppSelector(selectPermissions);
    hasAnyTeamPermission(permissionSet, permission);
}

export function useCheckProcessPermission(
    processId: number | undefined,
    permission: PermissionLike,
): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return checkProcessPermission(permissionSet, processId, permission);
}

export function useHasProcessPermission(
    processId: number | undefined,
    permission: PermissionLike,
): void {
    const permissionSet = useAppSelector(selectPermissions);
    hasProcessPermission(permissionSet, processId, permission);
}

export function useCheckProcessInstancePermission(
    processInstanceId: number | undefined,
    permission: PermissionLike,
): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return checkProcessInstancePermission(permissionSet, processInstanceId, permission);
}

export function useHasProcessInstancePermission(
    processInstanceId: number | undefined,
    permission: PermissionLike,
): void {
    const permissionSet = useAppSelector(selectPermissions);
    hasProcessInstancePermission(permissionSet, processInstanceId, permission);
}
