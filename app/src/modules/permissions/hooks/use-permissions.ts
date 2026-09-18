import {useCallback} from 'react';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectPermissions, selectUser, setPermissions} from '../../../slices/user-slice';
import {PermissionApiService} from '../permission-api-service';
import {broadcastCrossTabInvalidation} from '../../../hooks/use-cross-tab-invalidation';
import {
    hasAnyDepartmentPermission,
    hasAnyTeamPermission,
    hasDepartmentPermission,
    hasProcessInstancePermission,
    hasProcessPermission,
    hasSystemPermission,
    hasTeamPermission,
    requireAnyDepartmentPermission,
    requireAnyTeamPermission,
    requireDepartmentPermission,
    requireProcessInstancePermission,
    requireProcessPermission,
    requireSystemPermission,
    requireTeamPermission,
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

export function useHasSystemPermission(permission: PermissionLike): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return hasSystemPermission(permissionSet, permission);
}

export function useRequireSystemPermission(permission: PermissionLike): void {
    const permissionSet = useAppSelector(selectPermissions);
    requireSystemPermission(permissionSet, permission);
}

export function useHasDepartmentPermission(
    departmentId: number | undefined,
    permission: PermissionLike,
): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return hasDepartmentPermission(permissionSet, departmentId, permission);
}

export function useRequireDepartmentPermission(
    departmentId: number | undefined,
    permission: PermissionLike,
): void {
    const permissionSet = useAppSelector(selectPermissions);
    requireDepartmentPermission(permissionSet, departmentId, permission);
}

export function useHasAnyDepartmentPermission(permission: PermissionLike): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return hasAnyDepartmentPermission(permissionSet, permission);
}

export function useRequireAnyDepartmentPermission(permission: PermissionLike): void {
    const permissionSet = useAppSelector(selectPermissions);
    requireAnyDepartmentPermission(permissionSet, permission);
}

export function useHasTeamPermission(
    teamId: number | undefined,
    permission: PermissionLike,
): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return hasTeamPermission(permissionSet, teamId, permission);
}

export function useRequireTeamPermission(
    teamId: number | undefined,
    permission: PermissionLike,
): void {
    const permissionSet = useAppSelector(selectPermissions);
    requireTeamPermission(permissionSet, teamId, permission);
}

export function useHasAnyTeamPermission(permission: PermissionLike): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return hasAnyTeamPermission(permissionSet, permission);
}

export function useRequireAnyTeamPermission(permission: PermissionLike): void {
    const permissionSet = useAppSelector(selectPermissions);
    requireAnyTeamPermission(permissionSet, permission);
}

export function useHasProcessPermission(
    processId: number | undefined,
    permission: PermissionLike,
): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return hasProcessPermission(permissionSet, processId, permission);
}

export function useRequireProcessPermission(
    processId: number | undefined,
    permission: PermissionLike,
): void {
    const permissionSet = useAppSelector(selectPermissions);
    requireProcessPermission(permissionSet, processId, permission);
}

export function useHasProcessInstancePermission(
    processInstanceId: number | undefined,
    permission: PermissionLike,
): boolean {
    const permissionSet = useAppSelector(selectPermissions);
    return hasProcessInstancePermission(permissionSet, processInstanceId, permission);
}

export function useRequireProcessInstancePermission(
    processInstanceId: number | undefined,
    permission: PermissionLike,
): void {
    const permissionSet = useAppSelector(selectPermissions);
    requireProcessInstancePermission(permissionSet, processInstanceId, permission);
}
