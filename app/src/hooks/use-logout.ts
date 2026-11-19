import {AuthService} from '../services/auth-service';
import {beginExplicitEditorDraftLogout, stopEditorDraftForLogout} from '../services/editor-draft-login';

export function useLogout() {
    return async () => {
        void stopEditorDraftForLogout().catch(console.error);
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
            // Until the provider finishes logging out, other tabs can still save under the old account.
            // Invalidate that entire generation only when the logout request has settled.
            const cleanup = beginExplicitEditorDraftLogout().catch(console.error);
            AuthService.logout();
            // A suspended database transaction must not strand the user on an authenticated page.
            let timer: number | undefined;
            await Promise.race([cleanup, new Promise<void>((resolve) => { timer = window.setTimeout(resolve, 2_000); })]);
            window.clearTimeout(timer);
            window.location.href = `${window.location.origin}/staff?logout=true`;
        }
    };
}
