import {useNavigate} from 'react-router-dom';
import {useSelector} from 'react-redux';
import {useEffect} from 'react';
import {selectMemberships, selectUser} from '../slices/user-slice';
import {useAppDispatch} from './use-app-dispatch';
import {showErrorSnackbar} from '../slices/snackbar-slice';
import {DepartmentMembership} from "../models/entities/department-membership";
import {User} from "../models/entities/user";

export function useUserGuard(condition: (user?: User, memberships?: DepartmentMembership[]) => boolean): void {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const user = useSelector(selectUser);
    const memberships = useSelector(selectMemberships);

    useEffect(() => {
        if (!(condition(user, memberships))) {
            navigate('/');
            dispatch(showErrorSnackbar('Fehlende Berechtigung, um auf diese Seite zuzugreifen.'));
        }
    }, [navigate, user, memberships, condition, dispatch]);
}
