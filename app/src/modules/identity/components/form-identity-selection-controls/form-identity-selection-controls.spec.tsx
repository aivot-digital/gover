import {createTheme, ThemeProvider} from '@mui/material/styles';
import {act, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {createRef} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createDerivedRuntimeElementData} from '../../../../models/element-data';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import type {IdentitySelectionApi} from '../../models/identity-selection-api';
import type {IdentityCommunicationState, IdentitySlot} from '../../models/identity-slot';
import {
    FormIdentitySelectionControls,
    type FormIdentitySelectionControlsHandle,
} from './form-identity-selection-controls';

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

function slot(overrides?: Partial<IdentitySlot>): IdentitySlot {
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

function createIdentityApi(overrides?: Partial<IdentitySelectionApi>): IdentitySelectionApi {
    return {
        createIdentityProviderStartLink: vi.fn(() => '/identity/start/'),
        setEmailIdentity: vi.fn(),
        clearIdentity: vi.fn(),
        selectCommunication: vi.fn(),
        deriveCommunication: vi.fn(),
        ...overrides,
    };
}

function renderControls(identitySlot: IdentitySlot, apiOverrides?: Partial<IdentitySelectionApi>) {
    const onChange = vi.fn();
    const identityApi = createIdentityApi(apiOverrides);
    render(
        <ThemeProvider theme={createTheme()}>
            <FormIdentitySelectionControls
                slot={identitySlot}
                api={identityApi}
                onChange={onChange}
            />
        </ThemeProvider>,
    );
    return {identityApi, onChange};
}

describe('FormIdentitySelectionControls', () => {
    beforeEach(() => {
        dispatch.mockReset();
    });

    it('stores a direct email identity without selecting a communication provider', async () => {
        const emailSlot = slot({
            identityType: 'Email',
            emailAddress: 'customer@example.test',
            isReady: true,
            availableIdentityProviders: [{...provider, isAuthenticatedWithThis: false}],
        });
        const setEmailIdentity = vi.fn().mockResolvedValue(emailSlot);
        const selectCommunication = vi.fn();
        const {onChange} = renderControls(slot(), {setEmailIdentity, selectCommunication});
        const user = userEvent.setup();

        await user.type(screen.getByRole('textbox', {name: /E-Mail-Adresse/}), 'customer@example.test');
        await user.click(screen.getByRole('button', {name: 'Übernehmen'}));

        await waitFor(() => expect(setEmailIdentity).toHaveBeenCalledWith(
            'applicant',
            'customer@example.test',
        ));
        expect(selectCommunication).not.toHaveBeenCalled();
        expect(onChange).toHaveBeenCalledWith(emailSlot);
    });

    it('previews a provider choice and persists it only when explicitly confirmed', async () => {
        const initialCommunication: IdentityCommunicationState = {
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
        const deriveCommunication = vi.fn().mockResolvedValue(selectedCommunication);
        const selectCommunication = vi.fn().mockResolvedValue(selectedCommunication);
        const {onChange} = renderControls(slot({
            allowsEmail: false,
            identityType: 'IdentityProvider',
            availableIdentityProviders: [{...provider, isAuthenticatedWithThis: true}],
            communication: initialCommunication,
        }), {deriveCommunication, selectCommunication});
        const user = userEvent.setup();

        await user.click(screen.getByRole('radio', {name: /Postfach/}));
        await waitFor(() => expect(deriveCommunication).toHaveBeenCalledWith('applicant', 20, {}, ['ALL']));
        expect(selectCommunication).not.toHaveBeenCalled();

        await user.click(screen.getByRole('button', {name: 'Angaben zum Kommunikationsweg übernehmen'}));

        await waitFor(() => expect(selectCommunication).toHaveBeenCalledWith('applicant', 20, {}));
        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
            identityType: 'IdentityProvider',
            isReady: true,
            communication: selectedCommunication,
        }));
    });

    it('persists an incomplete communication draft before another identity login', async () => {
        const initialCommunication: IdentityCommunicationState = {
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
        const incompleteCommunication = {...initialCommunication, selectedBindingId: 20};
        const deriveCommunication = vi.fn().mockResolvedValue(incompleteCommunication);
        const selectCommunication = vi.fn().mockResolvedValue(incompleteCommunication);
        const onChange = vi.fn();
        const controlsRef = createRef<FormIdentitySelectionControlsHandle>();
        const user = userEvent.setup();
        render(
            <ThemeProvider theme={createTheme()}>
                <FormIdentitySelectionControls
                    ref={controlsRef}
                    slot={slot({
                        allowsEmail: false,
                        identityType: 'IdentityProvider',
                        availableIdentityProviders: [{...provider, isAuthenticatedWithThis: true}],
                        communication: initialCommunication,
                    })}
                    api={createIdentityApi({deriveCommunication, selectCommunication})}
                    onChange={onChange}
                    saveMode="deferred"
                />
            </ThemeProvider>,
        );

        await user.click(screen.getByRole('radio', {name: /Postfach/}));
        await waitFor(() => expect(deriveCommunication).toHaveBeenCalledOnce());

        let persisted = false;
        await act(async () => {
            persisted = await controlsRef.current!.persistPendingSelection();
        });

        expect(persisted).toBe(true);
        expect(selectCommunication).toHaveBeenCalledWith('applicant', 20, {});
        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
            isReady: false,
            communication: incompleteCommunication,
        }));
    });

    it('clears a selected identity so another authentication can be started', async () => {
        const clearIdentity = vi.fn().mockResolvedValue(undefined);
        const {onChange} = renderControls(slot({
            allowsEmail: false,
            identityType: 'IdentityProvider',
            isReady: true,
            availableIdentityProviders: [{...provider, isAuthenticatedWithThis: true}],
        }), {clearIdentity});
        const user = userEvent.setup();

        await user.click(screen.getByRole('button', {name: 'Identität entfernen'}));

        await waitFor(() => expect(clearIdentity).toHaveBeenCalledWith('applicant'));
        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
            identityType: null,
            emailAddress: null,
            isReady: false,
            communication: null,
            availableIdentityProviders: [expect.objectContaining({isAuthenticatedWithThis: false})],
        }));
    });
});
