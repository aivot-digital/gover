import {createTheme, ThemeProvider} from '@mui/material/styles';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createDerivedRuntimeElementData} from '../../../../models/element-data';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import {
    FormTriggerApiService,
    type FormIdentityCommunicationState,
    type FormIdentitySlot,
} from '../../../forms/services/form-trigger-api-service';
import {FormIdentitySelectionControls} from './form-identity-selection-controls';

const dispatch = vi.fn();

vi.mock('../../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => dispatch,
}));

vi.mock('../../../elements/components/element-derivation-context', () => ({
    ElementDerivationContext: () => <div>Provider fields</div>,
}));

const provider = {
    identityProviderKey: '36a9a19d-f9fb-4225-a9a0-07a223820b4b',
    identityProviderName: 'BundID',
    identityProviderAssetKey: null,
    identityProviderType: IdentityProviderType.BundID,
    isAuthenticatedWithThis: false,
    additionalScopes: [],
};

function slot(overrides?: Partial<FormIdentitySlot>): FormIdentitySlot {
    return {
        id: 'applicant',
        title: 'Antragsteller:in',
        description: null,
        isOptional: false,
        isRequired: true,
        allowsEmail: true,
        identityType: null,
        emailAddress: null,
        isReady: false,
        availableIdentityProviders: [provider],
        communication: null,
        ...overrides,
    };
}

function renderControls(identitySlot: FormIdentitySlot) {
    const onChange = vi.fn();
    const onPendingChange = vi.fn();
    render(
        <ThemeProvider theme={createTheme()}>
            <FormIdentitySelectionControls
                slot={identitySlot}
                processSlug="example-process"
                formSlug="example-form"
                relatedProcessNodeId={42}
                onChange={onChange}
                onPendingChange={onPendingChange}
            />
        </ThemeProvider>,
    );
    return {onChange, onPendingChange};
}

describe('FormIdentitySelectionControls', () => {
    beforeEach(() => {
        dispatch.mockReset();
        vi.restoreAllMocks();
    });

    it('stores a direct email identity without selecting a communication provider', async () => {
        const emailSlot = slot({
            identityType: 'Email',
            emailAddress: 'customer@example.test',
            isReady: true,
            availableIdentityProviders: [{...provider, isAuthenticatedWithThis: false}],
        });
        const setEmail = vi.spyOn(FormTriggerApiService.prototype, 'setEmailIdentity').mockResolvedValue(emailSlot);
        const selectCommunication = vi.spyOn(FormTriggerApiService.prototype, 'selectCommunication');
        const {onChange} = renderControls(slot());
        const user = userEvent.setup();

        await user.type(screen.getByRole('textbox', {name: /E-Mail-Adresse/}), 'customer@example.test');
        await user.click(screen.getByRole('button', {name: 'E-Mail-Adresse übernehmen'}));

        await waitFor(() => expect(setEmail).toHaveBeenCalledWith(
            'example-process',
            'example-form',
            'applicant',
            'customer@example.test',
            undefined,
        ));
        expect(selectCommunication).not.toHaveBeenCalled();
        expect(onChange).toHaveBeenCalledWith(emailSlot);
    });

    it('previews a provider choice and persists it only when explicitly confirmed', async () => {
        const initialCommunication: FormIdentityCommunicationState = {
            required: true,
            ready: false,
            selectedBindingId: null,
            choices: [
                {id: 10, name: 'E-Mail', description: 'Versand per E-Mail'},
                {id: 20, name: 'Postfach', description: 'Digitales Postfach'},
            ],
            customerLayout: null,
            customerData: {},
            derivedData: createDerivedRuntimeElementData(),
        };
        const selectedCommunication = {...initialCommunication, ready: true, selectedBindingId: 20};
        const derive = vi.spyOn(FormTriggerApiService.prototype, 'deriveCommunication')
            .mockResolvedValue(selectedCommunication);
        const select = vi.spyOn(FormTriggerApiService.prototype, 'selectCommunication')
            .mockResolvedValue(selectedCommunication);
        const {onChange, onPendingChange} = renderControls(slot({
            allowsEmail: false,
            identityType: 'IdentityProvider',
            availableIdentityProviders: [{...provider, isAuthenticatedWithThis: true}],
            communication: initialCommunication,
        }));
        const user = userEvent.setup();

        await user.click(screen.getByRole('radio', {name: /Postfach/}));
        await waitFor(() => expect(derive).toHaveBeenCalledWith('applicant', 42, 20, {}));
        expect(select).not.toHaveBeenCalled();

        await user.click(screen.getByRole('button', {name: 'Angaben zum Kommunikationsweg übernehmen'}));

        await waitFor(() => expect(select).toHaveBeenCalledWith('applicant', 42, 20, {}));
        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
            identityType: 'IdentityProvider',
            isReady: true,
            communication: selectedCommunication,
        }));
        expect(onPendingChange).toHaveBeenCalledWith(true);
        expect(onPendingChange).toHaveBeenLastCalledWith(false);
    });
});
