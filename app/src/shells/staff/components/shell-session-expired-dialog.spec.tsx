import {act, fireEvent, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, expect, it, vi} from 'vitest';
import {AuthService} from '../../../services/auth-service';
import {clearEditorDraftLoginReturn, registerEditorDraftLoginHandler} from '../../../services/editor-draft-login';
import {ShellSessionExpiredDialog} from './shell-session-expired-dialog';

afterEach(() => {
    vi.useRealTimers();
    clearEditorDraftLoginReturn();
    window.history.replaceState(null, '', '/');
});

it('prepares the draft once on login confirmation and waits for it before navigating', async () => {
    vi.useFakeTimers();
    vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(false);
    const getUrl = vi.spyOn(AuthService, 'getLoginUrl').mockReturnValue('#login');
    let finish!: (reserved: boolean) => void;
    const prepare = vi.fn((_id: string) => new Promise<boolean>((resolve) => { finish = resolve; }));
    const unregister = registerEditorDraftLoginHandler(prepare);
    try {
        render(<MemoryRouter initialEntries={['/editor/42']}><ShellSessionExpiredDialog/></MemoryRouter>);
        await act(() => vi.advanceTimersByTimeAsync(1_100));
        const login = screen.getByRole('button', {name: 'Erneut anmelden'});
        expect(prepare).not.toHaveBeenCalled();
        fireEvent.click(login);
        expect(login).toBeDisabled();
        fireEvent.click(login);
        expect(prepare).toHaveBeenCalledOnce();
        expect(getUrl).not.toHaveBeenCalled();

        await act(async () => { finish(true); });
        expect(getUrl).toHaveBeenCalledWith(expect.objectContaining({pathname: '/editor/42'}), prepare.mock.calls[0][0]);
        expect(window.location.hash).toBe('#login');
    } finally {
        unregister();
    }
});
