import {AuthService} from '../services/auth-service';

export function useLogout() {
    return async () => {
        try {
            if (AuthService.getCsrfToken() == null) {
                await AuthService.refresh();
            }

            const csrfToken = AuthService.getCsrfToken();
            await fetch('/api/auth/logout', {
                method: 'POST',
                credentials: 'same-origin',
                headers: csrfToken != null ? {
                    'X-CSRF-TOKEN': csrfToken,
                } : undefined,
            });
        } finally {
            AuthService.logout();
            window.location.href = `${window.location.origin}/staff?logout=true`;
        }
    };
}
