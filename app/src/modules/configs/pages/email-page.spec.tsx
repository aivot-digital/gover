import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {EmailPage} from './email-page';
import {
    MailApiService,
    type MailConfigurationResponseDTO,
} from '../mail-api-service';

vi.mock('../../permissions/hooks/use-permissions', () => ({
    useRequireSystemPermission: vi.fn(),
}));

vi.mock('../../../hooks/use-app-selector', () => ({
    useAppSelector: () => ({
        email: 'admin@example.com',
    }),
}));

const configuredMailService: MailConfigurationResponseDTO = {
    configured: true,
    host: 'smtp.example.com',
    port: 587,
    authenticationEnabled: true,
    username: 'prosuna@example.com',
    passwordConfigured: true,
    startTlsEnabled: true,
    senderName: 'Prosuna Service',
    senderAddress: 'service@example.com',
    configurationIssues: [],
};

describe('EmailPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        vi.spyOn(MailApiService.prototype, 'getConfiguration')
            .mockResolvedValue(configuredMailService);
    });

    it('renders the protected mail configuration details', async () => {
        render(<EmailPage/>);

        expect(await screen.findByText('smtp.example.com:587')).toBeInTheDocument();
        expect(screen.queryByText('Port')).not.toBeInTheDocument();
        expect(screen.getByText('STARTTLS aktiviert')).toBeInTheDocument();
        expect(screen.getByText('prosuna@example.com')).toBeInTheDocument();
        expect(screen.getByText('Prosuna Service <service@example.com>')).toBeInTheDocument();
        expect(screen.getByDisplayValue('admin@example.com')).toBeInTheDocument();
    });

    it('sends a test mail and reports SMTP acceptance', async () => {
        const sendTestMail = vi.spyOn(MailApiService.prototype, 'sendTestMail')
            .mockResolvedValue({
                success: true,
                errorMessage: null,
            });
        render(<EmailPage/>);

        const button = await screen.findByRole('button', {name: 'Test-E-Mail versenden'});
        fireEvent.click(button);

        await waitFor(() => {
            expect(sendTestMail).toHaveBeenCalledWith('admin@example.com');
        });
        expect(await screen.findByText(/hat die Test-E-Mail an admin@example.com/)).toBeInTheDocument();
    });

    it('disables the test for an incomplete configuration', async () => {
        vi.mocked(MailApiService.prototype.getConfiguration).mockResolvedValue({
            ...configuredMailService,
            configured: false,
            host: null,
            configurationIssues: ['Es ist kein SMTP-Server konfiguriert.'],
        });
        render(<EmailPage/>);

        expect(await screen.findByText('Es ist kein SMTP-Server konfiguriert.')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Test-E-Mail versenden'})).toBeDisabled();
    });

    it('shows the error returned by the mail server', async () => {
        vi.spyOn(MailApiService.prototype, 'sendTestMail')
            .mockResolvedValue({
                success: false,
                errorMessage: 'SMTP authentication failed',
            });
        render(<EmailPage/>);

        fireEvent.click(await screen.findByRole('button', {name: 'Test-E-Mail versenden'}));

        expect(await screen.findByText('SMTP authentication failed')).toBeInTheDocument();
        expect(screen.getByText('Testversand fehlgeschlagen')).toBeInTheDocument();
    });
});
