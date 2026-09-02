import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {SecretSelectDialog} from './secret-select-dialog';

const {secret} = vi.hoisted(() => ({secret: {
    key: 'mail-api',
    name: 'Mail-API',
    description: 'Zugang für den Mailversand',
    value: 'not-rendered',
}}));

vi.mock('../../../hooks/use-api', () => {
    const api = {};
    return {useApi: () => api};
});
vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../secrets-api-service', () => ({
    SecretsApiService: class {
        listAll = vi.fn().mockResolvedValue({content: [secret]});
    },
}));

describe('SecretSelectDialog', () => {
    it('lists secret references without exposing their values', async () => {
        const user = userEvent.setup();
        const onSelect = vi.fn();
        const onClose = vi.fn();

        render(
            <SecretSelectDialog
                open
                onClose={onClose}
                onSelect={onSelect}
            />,
        );

        const option = (await screen.findByText('Mail-API')).closest('[role="button"]');
        expect(option).not.toBeNull();
        expect(option).toHaveTextContent('Zugang für den Mailversand');
        expect(screen.queryByText('not-rendered')).not.toBeInTheDocument();

        await user.click(option!);
        expect(onSelect).toHaveBeenCalledWith(secret);
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
