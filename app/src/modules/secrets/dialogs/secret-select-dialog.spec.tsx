import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {SecretsApiService} from '../secrets-api-service';
import {SecretSelectDialog} from './secret-select-dialog';

const mocks = vi.hoisted(() => ({
    api: {},
    dispatch: vi.fn(),
}));

vi.mock('../../../hooks/use-api', () => ({
    useApi: () => mocks.api,
}));

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => mocks.dispatch,
}));

const secrets = [
    {
        key: 'production-secret',
        name: 'Produktionszugang',
        description: 'Nur für das Produktivsystem',
        value: 'production-password',
    },
    {
        key: 'test-secret',
        name: 'Testzugang',
        description: 'Nur für die Testumgebung',
        value: 'test-password',
    },
];

describe('SecretSelectDialog', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        mocks.dispatch.mockReset();
    });

    it('loads ordered references, searches metadata locally, and selects without displaying values', async () => {
        vi.spyOn(SecretsApiService.prototype, 'listAllOrdered').mockResolvedValue({
            content: secrets,
            page: {size: 2, number: 0, totalElements: 2, totalPages: 1},
        });
        const onSelect = vi.fn();
        const onClose = vi.fn();

        render(
            <SecretSelectDialog
                id="secret-dialog"
                open
                onSelect={onSelect}
                onClose={onClose}
            />,
        );

        expect(await screen.findByText('Produktionszugang')).toBeInTheDocument();
        expect(screen.getByText('Nur für das Produktivsystem')).toBeInTheDocument();
        expect(screen.queryByText('production-password')).not.toBeInTheDocument();
        expect(screen.queryByText('test-password')).not.toBeInTheDocument();
        expect(SecretsApiService.prototype.listAllOrdered).toHaveBeenCalledWith('name', 'ASC');

        fireEvent.change(screen.getByRole('searchbox', {name: 'Geheimnis suchen'}), {
            target: {value: 'Produktivsystem'},
        });

        expect(screen.getByText('Produktionszugang')).toBeInTheDocument();
        expect(screen.queryByText('Testzugang')).not.toBeInTheDocument();

        fireEvent.click(screen.getByText('Produktionszugang'));
        expect(onSelect).toHaveBeenCalledWith(secrets[0]);
        expect(onClose).toHaveBeenCalledOnce();
    });

    it('discards the prior list and refreshes whenever the dialog is reopened', async () => {
        const list = vi.spyOn(SecretsApiService.prototype, 'listAllOrdered')
            .mockResolvedValueOnce({
                content: [secrets[0]],
                page: {size: 1, number: 0, totalElements: 1, totalPages: 1},
            })
            .mockResolvedValueOnce({
                content: [secrets[1]],
                page: {size: 1, number: 0, totalElements: 1, totalPages: 1},
            });
        const props = {
            onSelect: vi.fn(),
            onClose: vi.fn(),
        };
        const {rerender} = render(<SecretSelectDialog open {...props}/>);

        expect(await screen.findByText('Produktionszugang')).toBeInTheDocument();

        rerender(<SecretSelectDialog open={false} {...props}/>);
        rerender(<SecretSelectDialog open {...props}/>);

        expect(await screen.findByText('Testzugang')).toBeInTheDocument();
        expect(screen.queryByText('Produktionszugang')).not.toBeInTheDocument();
        await waitFor(() => expect(list).toHaveBeenCalledTimes(2));
    });
});
