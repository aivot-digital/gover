import {createTheme, ThemeProvider} from '@mui/material/styles';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import {IdentityButton, type IdentityButtonProps} from './identity-button';

const defaultProps: IdentityButtonProps = {
    isAuthenticated: false,
    startUri: '/api/public/form/process/form/identities/person/providers/bund-id/start/',
    identityProviderAssetKey: null,
    identityProviderName: 'BundID',
    identityProviderType: IdentityProviderType.BundID,
};

function renderIdentityButton(props?: Partial<IdentityButtonProps>) {
    return render(
        <ThemeProvider theme={createTheme({palette: {mode: 'dark'}})}>
            <IdentityButton {...defaultProps} {...props}/>
        </ThemeProvider>,
    );
}

describe('IdentityButton', () => {
    beforeEach(() => {
        window.history.replaceState({}, '', '/');
    });

    it('renders an available provider as a login link', () => {
        renderIdentityButton();

        expect(screen.getByRole('link', {name: /Mit „BundID“ anmelden/})).toHaveAttribute(
            'href',
            defaultProps.startUri,
        );
        expect(screen.getByTestId('identity-provider-logo')).toHaveAttribute('aria-label', 'Logo BundID');
    });

    it('renders an authenticated provider as a visible non-interactive status', () => {
        renderIdentityButton({isAuthenticated: true});

        expect(screen.getByRole('status')).toHaveTextContent('Mit „BundID“ angemeldet');
        expect(screen.getByText('Angemeldet')).toBeInTheDocument();
        expect(screen.queryByRole('link')).not.toBeInTheDocument();
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('waits for pending identity data before following the login link', async () => {
        const beforeStart = vi.fn().mockResolvedValue(true);
        const user = userEvent.setup();
        renderIdentityButton({
            beforeStart,
            startUri: '#identity-provider-start',
        });

        await user.click(screen.getByRole('link', {name: /Mit „BundID“ anmelden/}));

        expect(beforeStart).toHaveBeenCalledOnce();
        await waitFor(() => expect(window.location.hash).toBe('#identity-provider-start'));
    });

    it('does not follow the login link when pending identity data cannot be persisted', async () => {
        const beforeStart = vi.fn().mockResolvedValue(false);
        const user = userEvent.setup();
        renderIdentityButton({
            beforeStart,
            startUri: '#identity-provider-start',
        });

        await user.click(screen.getByRole('link', {name: /Mit „BundID“ anmelden/}));

        await waitFor(() => expect(beforeStart).toHaveBeenCalledOnce());
        expect(window.location.hash).toBe('');
    });
});
