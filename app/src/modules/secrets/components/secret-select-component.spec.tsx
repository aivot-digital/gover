import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {SecretsApiService} from '../secrets-api-service';
import {SecretSelectComponent} from './secret-select-component';

const mocks = vi.hoisted(() => ({
    api: {},
}));

vi.mock('../../../hooks/use-api', () => ({
    useApi: () => mocks.api,
}));

vi.mock('../dialogs/secret-select-dialog', () => ({
    SecretSelectDialog: ({open, onSelect}: {
        open: boolean;
        onSelect: (secret: {key: string; name: string; description: string; value: string}) => void;
    }) => open ? (
        <div role="dialog" aria-label="Geheimnisauswahl">
            <button
                onClick={() => onSelect({
                    key: 'selected-secret',
                    name: 'Ausgewähltes Geheimnis',
                    description: 'Für den Produktionseinsatz',
                    value: 'must-not-be-rendered',
                })}
            >
                Geheimnis auswählen
            </button>
        </div>
    ) : null,
}));

describe('SecretSelectComponent', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
    });

    it('loads and displays secret metadata without exposing the secret value', async () => {
        vi.spyOn(SecretsApiService.prototype, 'retrieve').mockResolvedValue({
            key: 'secret-key',
            name: 'Produktionszugang',
            description: 'Zugang für das Produktivsystem',
            value: 'highly-sensitive-value',
        });

        render(
            <SecretSelectComponent
                label="API-Schlüssel"
                value=" secret-key "
                onChange={vi.fn()}
            />,
        );

        expect(await screen.findByText('Produktionszugang')).toBeInTheDocument();
        expect(screen.getByText('Zugang für das Produktivsystem')).toBeInTheDocument();
        expect(screen.queryByText('highly-sensitive-value')).not.toBeInTheDocument();
        expect(SecretsApiService.prototype.retrieve).toHaveBeenCalledWith('secret-key');
    });

    it('opens the dialog, saves only the selected key, and allows clearing', async () => {
        vi.spyOn(SecretsApiService.prototype, 'retrieve').mockResolvedValue({
            key: 'existing-secret',
            name: 'Bestehendes Geheimnis',
            description: '',
            value: 'must-not-be-rendered',
        });
        const onChange = vi.fn();

        render(
            <SecretSelectComponent
                label="Geheimnis"
                value="existing-secret"
                onChange={onChange}
            />,
        );

        await screen.findByText('Bestehendes Geheimnis');
        fireEvent.click(screen.getByRole('textbox', {name: 'Geheimnis'}));
        fireEvent.click(screen.getByRole('button', {name: 'Geheimnis auswählen'}));

        expect(onChange).toHaveBeenCalledWith('selected-secret');
        expect(screen.queryByText('must-not-be-rendered')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Auswahl entfernen'}));
        expect(onChange).toHaveBeenCalledWith(null);
    });

    it('preserves unavailable keys and flags them inline until they are replaced or cleared', async () => {
        vi.spyOn(SecretsApiService.prototype, 'retrieve').mockRejectedValue(new Error('Not found'));
        const onChange = vi.fn();

        render(
            <SecretSelectComponent
                label="Geheimnis"
                value="deleted-secret"
                onChange={onChange}
            />,
        );

        expect(await screen.findByText('deleted-secret')).toBeInTheDocument();
        expect(screen.getByText('Nicht verfügbar')).toBeInTheDocument();
        expect(screen.getByText('Das ausgewählte Geheimnis ist nicht verfügbar.')).toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', {name: 'Auswahl entfernen'}));
        expect(onChange).toHaveBeenCalledWith(null);
    });
});
