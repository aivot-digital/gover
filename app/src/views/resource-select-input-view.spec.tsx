import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {AssetSelectInputView} from './asset-select-input-view';
import {SecretSelectInputView} from './secret-select-input-view';
import {ElementType} from '../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {createDerivedRuntimeElementData} from '../models/element-data';

const mocks = vi.hoisted(() => ({dispatch: vi.fn(), api: {}}));
vi.mock('../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../hooks/use-api', () => ({useApi: () => mocks.api}));
vi.mock('../dialogs/select-asset-dialog/select-asset-dialog', () => ({SelectAssetDialog: () => null}));
vi.mock('../modules/secrets/dialogs/secret-select-dialog', () => ({SecretSelectDialog: () => null}));

describe.each([
    {name: 'Asset', type: ElementType.AssetSelectInput},
    {name: 'Secret', type: ElementType.SecretSelectInput},
])('$name input adapter', ({type}) => {
    function renderInput(options: {disabled?: boolean; isBusy?: boolean; isDeriving?: boolean; derivable?: boolean; errors?: string[]} = {}) {
        const element = {
            label: 'Referenz',
            hint: 'Eine hinterlegte Ressource auswählen.',
            required: true,
            disabled: options.disabled,
            value: options.derivable ? {
                type: 'Javascript' as const, requirements: null, noCode: null, referencedIds: [],
                javascriptCode: {code: 'return null;'},
            } : undefined,
        };
        const sharedProps = {
            element,
            value: null,
            setValue: vi.fn(), onBlur: vi.fn(),
            authoredElementValues: {}, onAuthoredElementValuesChange: vi.fn(),
            derivedData: createDerivedRuntimeElementData(),
            onDerive: vi.fn(), onEvent: vi.fn(), onResetErrors: vi.fn(),
            derivationTriggerIdQueue: [], suppressErrors: false,
            isBusy: options.isBusy ?? false, isDeriving: options.isDeriving ?? false,
            errors: options.errors,
        };
        // Both adapters share the view contract but intentionally retain distinct element-specific options.
        render(type === ElementType.AssetSelectInput
            ? <AssetSelectInputView {...sharedProps} element={{...generateElementWithDefaultValues(ElementType.AssetSelectInput), ...element}}/>
            : <SecretSelectInputView {...sharedProps} element={{...generateElementWithDefaultValues(ElementType.SecretSelectInput), ...element}}/>);
        return screen.getByRole('button', {name: /^Referenz (?!.*Auswahl entfernen)/});
    }

    it('links one field error to the required selection control', () => {
        const control = renderInput({errors: ['Ressource fehlt.']});
        expect(control).toHaveAttribute('aria-invalid', 'true');
        expect(control).toHaveAttribute('aria-required', 'true');
        expect(control).toHaveAccessibleDescription('Ressource fehlt. Erforderliche Auswahl.');
        expect(screen.getAllByRole('alert')).toHaveLength(1);
    });

    it.each([
        {disabled: true},
        {isBusy: true},
        {isDeriving: true, derivable: true},
    ])('prevents selection for %j', options => {
        expect(renderInput(options)).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Referenz: Auswahl entfernen'})).toBeDisabled();
    });

    it('keeps unrelated fields editable during derivation', () => {
        expect(renderInput({isDeriving: true})).toBeEnabled();
    });
});
