import {AuthService} from '../services/auth-service';
import {EditorDraftStorage} from '../services/editor-draft-storage';

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
            // An explicit logout leaves no unsaved changes behind, e.g. on a shared computer. An expired session keeps
            // them, so they can be offered again after the next login.
            await EditorDraftStorage.clear().catch(console.error);
            AuthService.logout();
            window.location.href = `${window.location.origin}/staff?logout=true`;
        }
    };
}
