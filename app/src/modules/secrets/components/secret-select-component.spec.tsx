import {useState} from 'react';
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
    SecretSelectDialog: ({id, open, onSelect}: {
        id?: string;
        open: boolean;
        onSelect: (secret: {key: string; name: string; description: string; value: string}) => void;
    }) => open ? (
        <div id={id} role="dialog" aria-label="Geheimnisauswahl">
            <button
                type="button"
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

        function TestField() {
            const [value, setValue] = useState<string | null>('existing-secret');

            return (
                <SecretSelectComponent
                    label="Geheimnis"
                    value={value}
                    onChange={(nextValue) => {
                        onChange(nextValue);
                        setValue(nextValue);
                    }}
                />
            );
        }

        render(<TestField/>);

        await screen.findByText('Bestehendes Geheimnis');
        fireEvent.click(getSelectionControl('Geheimnis'));
        fireEvent.click(screen.getByRole('button', {name: 'Geheimnis auswählen'}));

        expect(onChange).toHaveBeenCalledWith('selected-secret');
        expect(screen.queryByText('must-not-be-rendered')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Geheimnis: Auswahl entfernen'}));
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

        fireEvent.click(screen.getByRole('button', {name: 'Geheimnis: Auswahl entfernen'}));
        expect(onChange).toHaveBeenCalledWith(null);
    });

    it('provides an accessible labelled control for selecting and clearing a reference', async () => {
        const onChange = vi.fn();

        function TestField() {
            const [value, setValue] = useState<string | null>(null);

            return (
                <SecretSelectComponent
                    label="API-Geheimnis"
                    value={value}
                    onChange={(nextValue) => {
                        onChange(nextValue);
                        setValue(nextValue);
                    }}
                    hint="Das Geheimnis wird nur referenziert."
                />
            );
        }

        render(<TestField/>);

        const control = getSelectionControl('API-Geheimnis');
        expect(control).toHaveAccessibleName('API-Geheimnis – optional Kein Geheimnis ausgewählt');
        expect(control).toHaveAccessibleDescription('Das Geheimnis wird nur referenziert.');

        fireEvent.click(control);
        fireEvent.click(screen.getByRole('button', {name: 'Geheimnis auswählen'}));
        expect(onChange).toHaveBeenCalledWith('selected-secret');
        expect(screen.queryByText('must-not-be-rendered')).not.toBeInTheDocument();

        expect(await screen.findByText('Ausgewähltes Geheimnis')).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'API-Geheimnis: Auswahl entfernen'}));
        expect(onChange).toHaveBeenCalledWith(null);
    });
});

function getSelectionControl(label: string): HTMLElement {
    const labelElement = screen.getByTitle(label);
    const control = document.getElementById(labelElement.getAttribute('for')!);
    expect(control).not.toBeNull();
    return control!;
}
