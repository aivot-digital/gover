import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {views} from './index';
import {ElementType} from '../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {createDerivedRuntimeElementData} from '../models/element-data';

vi.mock('../hooks/use-api', () => ({
    useApi: () => ({}),
}));

vi.mock('../modules/secrets/dialogs/secret-select-dialog', () => ({
    SecretSelectDialog: (props: {
        open: boolean;
        onSelect: (secret: {key: string; name: string; description: string}) => void;
    }) => props.open ? (
        <div role="dialog" aria-label="Geheimnisauswahl">
            <button type="button" onClick={() => props.onSelect({
                key: 'api-secret',
                name: 'API-Zugang',
                description: '',
            })}>
                API-Zugang auswählen
            </button>
        </div>
    ) : null,
}));

describe('SecretSelectInputView', () => {
    it('keeps backend-defined secret selectors usable through the view registry', async () => {
        const user = userEvent.setup();
        const setValue = vi.fn();
        const SecretView = views[ElementType.SecretSelectInput]!;

        render(
            <SecretView
                element={{
                    ...generateElementWithDefaultValues(ElementType.SecretSelectInput),
                    label: 'API-Schlüssel',
                    hint: 'Wählen Sie den Zugang für den Dienst aus.',
                    required: true,
                }}
                value={null}
                setValue={setValue}
                onBlur={vi.fn()}
                authoredElementValues={{}}
                onAuthoredElementValuesChange={vi.fn()}
                derivedData={createDerivedRuntimeElementData()}
                onDerive={vi.fn()}
                onEvent={vi.fn()}
                onResetErrors={vi.fn()}
                derivationTriggerIdQueue={[]}
                suppressErrors={false}
                isBusy={false}
                isDeriving={false}
            />,
        );

        const control = screen.getByRole('button', {name: /^API-Schlüssel /});
        expect(control).toBeEnabled();
        expect(control).toHaveAccessibleDescription(/Wählen Sie den Zugang für den Dienst aus\./);
        await user.click(control);
        await user.click(screen.getByRole('button', {name: 'API-Zugang auswählen'}));

        expect(setValue).toHaveBeenCalledWith('api-secret');
    });
});
