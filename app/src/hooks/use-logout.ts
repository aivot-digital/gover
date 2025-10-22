import {useAppDispatch} from './use-app-dispatch';
import {AuthService} from '../services/auth-service';
import {setMemberships, setUser} from '../slices/user-slice';
import {setStatus, ShellStatus} from '../slices/shell-slice';
import {useNavigate} from 'react-router-dom';
import {createOidcPath} from '../utils/create-oidc-path';

export function useLogout() {
    const dispatch = useAppDispatch();

    return () => {
        new AuthService().logout();
        dispatch(setUser(undefined));
        dispatch(setMemberships([]));
        dispatch(setStatus(ShellStatus.Login));

        const searchParams = new URLSearchParams({
            post_logout_redirect_uri: window.location.origin + '/staff',
            client_id: AppConfig.oidc.client,
        });

        window.location.href = createOidcPath(`/realms/${AppConfig.oidc.realm}/protocol/openid-connect/logout?${searchParams.toString()}`);
    };
}