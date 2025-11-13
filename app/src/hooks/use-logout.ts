import {createOidcPath} from '../utils/create-oidc-path';

export function useLogout() {
    return () => {
        const searchParams = new URLSearchParams({
            post_logout_redirect_uri: window.location.origin + '/staff?logout=true',
            client_id: AppConfig.oidc.client,
        });
        window.location.href = createOidcPath(`/realms/${AppConfig.oidc.realm}/protocol/openid-connect/logout?${searchParams.toString()}`);
    };
}