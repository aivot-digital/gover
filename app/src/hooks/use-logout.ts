import {AuthService} from '../services/auth-service';

export function useLogout() {
    return async () => {
        try {
            await fetch('/api/auth/logout', {
                method: 'GET',
                credentials: 'same-origin',
            });
        } finally {
            AuthService.logout();
            window.location.href = `${window.location.origin}/staff?logout=true`;
        }
    };
}
