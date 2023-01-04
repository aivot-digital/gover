import {useNavigate} from 'react-router-dom';
import {useSelector} from 'react-redux';
import {useEffect} from 'react';
import {UserRole} from '../data/user-role';
import {selectUser} from '../slices/user-slice';
import {useAppDispatch} from './use-app-dispatch';
import {showErrorSnackbar} from '../slices/snackbar-slice';
import {checkUserRole} from '../utils/check-user-role';

export function useRoleGuard(minRole: UserRole): void {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const user = useSelector(selectUser);

    useEffect(() => {
        if (!checkUserRole(minRole, user)) {
            navigate('/');
            dispatch(showErrorSnackbar('Fehlende Berechtigung, um auf diese Seite zuzugreifen.'));
        }
    }, [navigate, user, minRole, dispatch]);
}
