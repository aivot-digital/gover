import {useAppDispatch} from './use-app-dispatch';
import {AuthService} from '../services/auth-service';
import {setMemberships, setUser} from '../slices/user-slice';
import {setStatus, ShellStatus} from '../slices/shell-slice';
import {useNavigate} from 'react-router-dom';

export function useLogout() {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();

    return () => {
        new AuthService().logout();
        dispatch(setUser(undefined));
        dispatch(setMemberships([]));
        dispatch(setStatus(ShellStatus.Login));
        navigate('/');
    };
}