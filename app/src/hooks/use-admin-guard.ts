import {useNavigate} from 'react-router-dom';
import {useSelector} from 'react-redux';
import {useEffect} from 'react';
import {selectUser} from '../slices/user-slice';
import {useAppDispatch} from './use-app-dispatch';
import {showErrorSnackbar} from '../slices/snackbar-slice';
import {isAdmin} from '../utils/is-admin';

export function useAdminGuard(): void {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const user = useSelector(selectUser);

    useEffect(() => {
        if (!isAdmin(user)) {
            navigate('/');
            dispatch(showErrorSnackbar('Sie müssen globale Administrator:in sein, um auf diese Seite zuzugreifen.'));
        }
    }, [navigate, user, dispatch]);
}
