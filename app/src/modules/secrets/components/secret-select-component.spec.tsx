import {useState} from 'react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {SecretSelectComponent} from './secret-select-component';

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
        retrieve = vi.fn().mockResolvedValue(secret);
    },
}));
vi.mock('../dialogs/secret-select-dialog', () => ({
    SecretSelectDialog: (props: {
        id?: string;
        open: boolean;
        onSelect: (value: typeof secret) => void;
    }) => props.open ? (
        <div id={props.id} role="dialog" aria-label="Geheimnis auswählen">
            <button type="button" onClick={() => props.onSelect(secret)}>Mail-API auswählen</button>
        </div>
    ) : null,
}));

describe('SecretSelectComponent', () => {
    it('keeps the secret value hidden and supports selecting and clearing a reference', async () => {
        const user = userEvent.setup();
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

        render(<TestField />);

        const label = screen.getByTitle('API-Geheimnis');
        const control = document.getElementById(label.getAttribute('for')!);
        expect(control).toHaveAccessibleName('API-Geheimnis – optional Kein Geheimnis ausgewählt');
        expect(control).toHaveAccessibleDescription('Das Geheimnis wird nur referenziert.');

        await user.click(control!);
        await user.click(screen.getByRole('button', {name: 'Mail-API auswählen'}));
        expect(onChange).toHaveBeenCalledWith('mail-api');
        expect(screen.queryByText('not-rendered')).not.toBeInTheDocument();

        await screen.findByText('Mail-API');
        await user.click(screen.getByRole('button', {name: 'API-Geheimnis: Auswahl entfernen'}));
        expect(onChange).toHaveBeenCalledWith(null);
    });
});
