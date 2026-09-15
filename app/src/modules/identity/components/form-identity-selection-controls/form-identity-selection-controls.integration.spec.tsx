import {createTheme, ThemeProvider} from '@mui/material/styles';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../../data/element-type/element-type';
import {
    ComputedElementValueSource,
    createDerivedRuntimeElementData,
} from '../../../../models/element-data';
import type {GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import type {IdentitySelectionApi} from '../../models/identity-selection-api';
import type {IdentityCommunicationState, IdentitySlot} from '../../models/identity-slot';
import {FormIdentitySelectionControls} from './form-identity-selection-controls';

vi.mock('../../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../../../hooks/use-app-selector', () => ({
    useAppSelector: () => false,
}));

describe('FormIdentitySelectionControls integration', () => {
    it('keeps an unmapped email field editable without starting a derivation loop', async () => {
        const deriveCommunication = vi.fn().mockImplementation(async () => manualCommunicationState());
        const user = userEvent.setup();

        renderControls(identitySlot({
            communication: unselectedCommunicationState(),
        }), {deriveCommunication});

        await user.click(screen.getByRole('radio', {name: /E-Mail/}));

        const email = await screen.findByRole('textbox', {name: /E-Mail-Adresse/});
        expect(email).toBeEnabled();
        expect(screen.queryByText('Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.')).not.toBeInTheDocument();
        await user.type(email, 'customer@example.test');
        await user.tab();
        expect(email).toHaveValue('customer@example.test');
        await waitFor(() => expect(deriveCommunication).toHaveBeenCalledTimes(1));
        expect(deriveCommunication).toHaveBeenCalledWith('applicant', 10, {}, ['ALL']);
    });

    it('shows a mapped email address in a disabled field without deriving again on mount', async () => {
        const deriveCommunication = vi.fn();

        renderControls(identitySlot({
            isReady: true,
            communication: mappedCommunicationState(),
        }), {deriveCommunication});

        const email = screen.getByRole('textbox', {name: /E-Mail-Adresse/});
        expect(email).toHaveValue('customer@example.test');
        expect(email).toBeDisabled();
        expect(screen.getByText(/aus Ihrem Nutzerkonto übernommen/)).toBeVisible();
        await waitFor(() => expect(deriveCommunication).not.toHaveBeenCalled());
    });

    it('shows full validation errors returned by an unsuccessful save', async () => {
        const selectCommunication = vi.fn().mockResolvedValue(manualCommunicationState({
            derivedData: createDerivedRuntimeElementData({
                effectiveValues: {email: null},
                elementStates: {
                    'mail-customer-config': {visible: true},
                    email: {
                        visible: true,
                        disabled: false,
                        valueSource: ComputedElementValueSource.Authored,
                        error: 'Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.',
                    },
                },
            }),
        }));
        const user = userEvent.setup();

        renderControls(identitySlot({
            communication: manualCommunicationState(),
        }), {selectCommunication});

        expect(screen.queryByText('Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.')).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Angaben zum Kommunikationsweg übernehmen'}));

        expect(await screen.findByText('Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.')).toBeVisible();
        expect(selectCommunication).toHaveBeenCalledWith('applicant', 10, {});
    });
});

function renderControls(identity: IdentitySlot, apiOverrides: Partial<IdentitySelectionApi>) {
    const api: IdentitySelectionApi = {
        createIdentityProviderStartLink: vi.fn(() => '/identity/start/'),
        setEmailIdentity: vi.fn(),
        clearIdentity: vi.fn(),
        selectCommunication: vi.fn(),
        deriveCommunication: vi.fn(),
        ...apiOverrides,
    };

    render(
        <ThemeProvider theme={createTheme()}>
            <FormIdentitySelectionControls
                slot={identity}
                api={api}
                onChange={vi.fn()}
            />
        </ThemeProvider>,
    );
}

function identitySlot(overrides: Partial<IdentitySlot>): IdentitySlot {
    return {
        id: 'applicant',
        title: 'Antragsteller:in',
        description: null,
        isOptional: false,
        isRequired: true,
        allowsEmail: false,
        identityType: 'IdentityProvider',
        emailAddress: null,
        isReady: false,
        availableIdentityProviders: [{
            identityProviderKey: '36a9a19d-f9fb-4225-a9a0-07a223820b4b',
            identityProviderName: 'BundID',
            identityProviderAssetKey: null,
            identityProviderType: IdentityProviderType.BundID,
            isAuthenticatedWithThis: true,
            additionalScopes: [],
        }],
        communication: null,
        ...overrides,
    };
}

function unselectedCommunicationState(): IdentityCommunicationState {
    return {
        required: true,
        ready: false,
        selectedBindingId: null,
        choices: [{id: 10, name: 'E-Mail', description: 'Versand per E-Mail'}],
        customerLayout: null,
        customerData: {},
        derivedData: createDerivedRuntimeElementData(),
    };
}

function manualCommunicationState(
    overrides: Partial<IdentityCommunicationState> = {},
): IdentityCommunicationState {
    return {
        ...unselectedCommunicationState(),
        selectedBindingId: 10,
        customerLayout: emailLayout(false),
        derivedData: createDerivedRuntimeElementData({
            effectiveValues: {email: null},
            elementStates: {
                'mail-customer-config': {visible: true},
                email: {
                    visible: true,
                    disabled: false,
                    valueSource: ComputedElementValueSource.Authored,
                },
            },
        }),
        ...overrides,
    };
}

function mappedCommunicationState(): IdentityCommunicationState {
    return {
        ...manualCommunicationState(),
        ready: true,
        customerLayout: emailLayout(true),
        derivedData: createDerivedRuntimeElementData({
            effectiveValues: {email: 'customer@example.test'},
            elementStates: {
                'mail-customer-config': {visible: true},
                email: {
                    visible: true,
                    disabled: true,
                    valueSource: ComputedElementValueSource.Derived,
                },
            },
        }),
    };
}

function emailLayout(disabled: boolean): GroupLayout {
    return {
        id: 'mail-customer-config',
        type: ElementType.GroupLayout,
        children: [{
            id: 'email',
            type: ElementType.Text,
            label: 'E-Mail-Adresse',
            hint: disabled
                ? 'Diese E-Mail-Adresse wurde aus Ihrem Nutzerkonto übernommen und kann hier nicht geändert werden.'
                : 'An diese Adresse werden Nachrichten zu Ihrem Vorgang gesendet.',
            autocomplete: 'email',
            required: true,
            disabled,
            technical: false,
        } as never],
    } as unknown as GroupLayout;
}
