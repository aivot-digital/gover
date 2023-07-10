import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { useEffect } from 'react';
import { selectMemberships, selectUser } from '../slices/user-slice';
import { useAppDispatch } from './use-app-dispatch';
import { showErrorSnackbar } from '../slices/snackbar-slice';
import { type DepartmentMembership } from '../models/entities/department-membership';
import { UserRole } from '../data/user-role';

export function useMembershipGuard(departmentId: number): void {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const user = useSelector(selectUser);
    const memberships = useSelector(selectMemberships);

    useEffect(() => {
        if (!(user?.admin ?? false) && !memberships?.some((membership: DepartmentMembership) => membership.department === departmentId && membership.role === UserRole.Admin)) {
            navigate('/');
            dispatch(showErrorSnackbar('Fehlende Berechtigung, um auf diese Seite zuzugreifen.'));
        }
    }, [navigate, user, memberships, dispatch]);
}
