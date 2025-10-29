import {useEffect, useMemo} from 'react';
import {selectMemberships, selectUser} from '../slices/user-slice';
import {useAppDispatch} from './use-app-dispatch';
import {isAdmin} from '../utils/is-admin';
import {addSnackbarMessage, removeSnackbarMessage, selectErrorMessage, setErrorMessage, SnackbarSeverity, SnackbarType} from '../slices/shell-slice';
import {UserRole} from '../data/user-role';
import {useAppSelector} from './use-app-selector';


export function useUser() {
    return useAppSelector(selectUser);
}

export function useUserIsAdmin(): boolean {
    const user = useUser();
    return isAdmin(user);
}

export function useMemberships() {
    return useAppSelector(selectMemberships);
}

export function useUserIsMemberOfDepartment(departmentId: number): boolean {
    const memberships = useMemberships();
    return memberships?.some(dept => dept.id === departmentId) ?? false;
}

interface Options {
    onlyGlobalAdmin?: true;
    onlyAdminOfDepartmentId?: number;
    messageType?: 'snackbar' | 'fullscreen';
}

const AccessDeniedSnackbarKey = 'access-denied';

export function useAccessGuard(options: Options): boolean {
    const dispatch = useAppDispatch();

    const user = useUser();
    const memberships = useMemberships();

    const errorMessage = useAppSelector(selectErrorMessage)

    const {
        onlyGlobalAdmin,
        onlyAdminOfDepartmentId,
        messageType,
    } = options;

    const hasAccess = useMemo(() => {
        if (isAdmin(user)) {
            return true;
        }

        if (onlyAdminOfDepartmentId != null) {
            if (memberships == null) {
                return false;
            }

            return memberships
                .some(dept => (
                    dept.id === onlyAdminOfDepartmentId &&
                    dept.role === UserRole.Admin
                )) ?? false;
        }

        return false;
    }, [user, memberships, onlyGlobalAdmin, onlyAdminOfDepartmentId]);

    useEffect(() => {
        if (hasAccess) {
            dispatch(removeSnackbarMessage(AccessDeniedSnackbarKey));
            if (errorMessage != null && errorMessage.status === 403) {
                dispatch(setErrorMessage(undefined));
            }
        } else {
            if (messageType === 'fullscreen') {
                dispatch(setErrorMessage({
                    status: 403,
                    message: 'Zugriff verweigert. Sie haben nicht die erforderlichen Berechtigungen, um auf diese Seite zuzugreifen.',
                }));
            } else if (messageType === 'snackbar') {
                dispatch(addSnackbarMessage({
                    key: AccessDeniedSnackbarKey,
                    message: 'Dieser Bereich kann nur von Administrator:innen bearbeitet werden.',
                    type: SnackbarType.Dismissable,
                    severity: SnackbarSeverity.Warning,
                }));
            }
        }
    }, [hasAccess, errorMessage, messageType]);

    return hasAccess;
}