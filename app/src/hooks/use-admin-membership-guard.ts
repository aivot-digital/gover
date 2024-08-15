import {useNavigate} from 'react-router-dom';
import {useSelector} from 'react-redux';
import {useEffect} from 'react';
import {selectMemberships, selectUser} from '../slices/user-slice';
import {useAppDispatch} from './use-app-dispatch';
import {showErrorSnackbar} from '../slices/snackbar-slice';
import {UserRole} from '../data/user-role';
import {isAdmin} from '../utils/is-admin';

export function useAdminMembershipGuard(departmentId?: number): void {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const user = useSelector(selectUser);
    const memberships = useSelector(selectMemberships);

    useEffect(() => {
        const isDepartmentAdmin = (
            isAdmin(user) || (
                departmentId != null &&
                (memberships ?? []).some((membership) => {
                    return (
                        membership.role === UserRole.Admin &&
                        membership.departmentId === departmentId
                    );
                })
            ) || (
                departmentId == null &&
                (memberships ?? []).some((membership) => {
                    return (
                        membership.role === UserRole.Admin
                    );
                })
            )
        );

        if (!isDepartmentAdmin) {
            navigate('/');
            dispatch(showErrorSnackbar('Sie müssen Fachbereichs-Administrator:in sein, um auf diese Seite zuzugreifen.'));
        }
    }, [navigate, user, memberships, dispatch]);
}
