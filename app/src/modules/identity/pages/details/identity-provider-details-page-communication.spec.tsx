import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {CommunicationProvidersApiService} from '../../../communication/communication-providers-api-service';
import {type CommunicationProvider, type CommunicationProviderBinding, type CommunicationProviderDefinition} from '../../../communication/models';
import {Permission} from '../../../../data/permissions/permission';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import {IdentityProviderDetailsPageCommunication} from './identity-provider-details-page-communication';

const testState = vi.hoisted(() => ({
    dispatch: vi.fn(),
    denied: [] as string[],
    identityProvider: {
        key: '00000000-0000-0000-0000-000000000001',
        type: 'BundId',
        isTestProvider: true,
    } as Record<string, any>,
}));

vi.mock('../../../../components/generic-details-page/generic-details-page-context', () => ({
    useGenericDetailsPageContext: () => ({
        item: testState.identityProvider,
        isBusy: false,
    }),
}));

vi.mock('../../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => testState.dispatch,
}));

vi.mock('../../../../providers/confirm-provider', () => ({
    useConfirm: () => vi.fn(),
}));

vi.mock('../../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: (permission: string) => !testState.denied.includes(permission),
}));

vi.mock('../../../elements/components/element-derivation-context', () => ({
    ElementDerivationContext: ({authoredElementValues, onAuthoredElementValuesChange}: any) => (
        <input aria-label="Attributzuordnung" value={authoredElementValues.attribute ?? ''} onChange={event => onAuthoredElementValuesChange({attribute: event.target.value})}/>
    ),
}));

describe('IdentityProviderDetailsPageCommunication', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        testState.dispatch.mockReset();
        testState.denied = [];
        testState.identityProvider = {
            key: '00000000-0000-0000-0000-000000000001',
            type: IdentityProviderType.BundID,
            isTestProvider: true,
        };
    });

    it('offers only active supported providers regardless of test status', async () => {
        const providers: CommunicationProvider[] = [
            provider(7, 'Produktiver Anbieter', true, false),
            provider(8, 'Inaktiver Testanbieter', false, true),
            provider(9, 'Aktiver Testanbieter', true, true),
            {...provider(10, 'Inkompatibler Anbieter', true, false), communicationProviderDefinitionKey: 'unsupported'},
        ];
        const definitions: CommunicationProviderDefinition[] = [{
            key: 'mail',
            version: 1,
            name: 'Mail',
            description: 'Mail',
            supportedIdentityProviderTypes: [IdentityProviderType.BundID],
        }];
        vi.spyOn(CommunicationProvidersApiService.prototype, 'listProviders').mockResolvedValue(providers);
        vi.spyOn(CommunicationProvidersApiService.prototype, 'listDefinitions').mockResolvedValue(definitions);
        vi.spyOn(CommunicationProvidersApiService.prototype, 'listBindings').mockResolvedValue([]);

        render(<IdentityProviderDetailsPageCommunication/>);

        const addButton = screen.getByRole('button', {name: 'Anbindung hinzufügen'});
        await waitFor(() => expect(addButton).toBeEnabled());
        fireEvent.click(addButton);
        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Kommunikationsanbieter'}));

        expect(await screen.findByText('Produktiver Anbieter')).toBeInTheDocument();
        expect(screen.queryByText('Inaktiver Testanbieter')).not.toBeInTheDocument();
        expect(screen.getByText('Aktiver Testanbieter')).toBeInTheDocument();
        expect(screen.queryByText('Inkompatibler Anbieter')).not.toBeInTheDocument();
    });

    function mockBindings(bindings: CommunicationProviderBinding[]) {
        vi.spyOn(CommunicationProvidersApiService.prototype, 'listProviders').mockResolvedValue([
            provider(7, 'Inaktiver Anbieter', false, false),
        ]);
        vi.spyOn(CommunicationProvidersApiService.prototype, 'listDefinitions').mockResolvedValue([]);
        vi.spyOn(CommunicationProvidersApiService.prototype, 'listBindings').mockResolvedValue(bindings);
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getBindingConfigurationLayout').mockResolvedValue({id: 'binding-layout', children: []} as any);
    }

    function binding(): CommunicationProviderBinding {
        return {
            id: 12,
            identityProviderKey: testState.identityProvider.key,
            communicationProviderId: 7,
            name: 'Mein Postfach',
            description: 'Nachrichten zum Vorgang',
            position: 5,
            isEnabled: true,
            configuration: {},
        };
    }

    it('keeps inactive existing providers visible and enables saving only while changes exist', async () => {
        mockBindings([binding()]);
        render(<IdentityProviderDetailsPageCommunication/>);
        fireEvent.click(await screen.findByRole('button', {name: 'Mein Postfach bearbeiten'}));

        expect(screen.getByRole('combobox', {name: 'Kommunikationsanbieter'})).toHaveTextContent('Inaktiver Anbieter');
        expect(screen.getByRole('combobox', {name: 'Kommunikationsanbieter'})).toHaveAttribute('aria-disabled', 'true');
        const save = screen.getByRole('button', {name: 'Speichern'});
        const name = screen.getByRole('textbox', {name: 'Anzeigename'});
        expect(save).toBeDisabled();
        fireEvent.change(name, {target: {value: 'Anderes Postfach'}});
        await waitFor(() => expect(save).toBeEnabled());
        fireEvent.change(name, {target: {value: 'Mein Postfach'}});
        expect(save).toBeDisabled();
        fireEvent.click(screen.getByRole('switch', {name: 'Aktiv'}));
        await waitFor(() => expect(save).toBeEnabled());
        fireEvent.click(screen.getByRole('switch', {name: 'Aktiv'}));
        expect(save).toBeDisabled();
    });

    it('edits bindings without submitting a manual position', async () => {
        mockBindings([binding()]);
        const update = vi.spyOn(CommunicationProvidersApiService.prototype, 'updateBinding').mockResolvedValue(binding());
        render(<IdentityProviderDetailsPageCommunication/>);
        fireEvent.click(await screen.findByRole('button', {name: 'Mein Postfach bearbeiten'}));
        expect(screen.queryByRole('textbox', {name: 'Reihenfolge'})).not.toBeInTheDocument();
        fireEvent.change(screen.getByRole('textbox', {name: 'Anzeigename'}), {target: {value: 'Neu'}});
        await waitFor(() => expect(screen.getByRole('button', {name: 'Speichern'})).toBeEnabled());
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        await waitFor(() => expect(update).toHaveBeenCalled());
        expect(update.mock.calls[0][1]).not.toHaveProperty('position');
        expect(update.mock.calls[0][1].name).toBe('Neu');
    });

    it('reorders the complete list and prevents overlapping mutations', async () => {
        const first = binding();
        const second = {...binding(), id: 13, name: 'Alternative', isEnabled: false};
        mockBindings([first, second]);
        let resolveOrder!: (value: CommunicationProviderBinding[]) => void;
        const reorder = vi.spyOn(CommunicationProvidersApiService.prototype, 'reorderBindings').mockReturnValue(new Promise(resolve => { resolveOrder = resolve; }));
        render(<IdentityProviderDetailsPageCommunication/>);
        const up = await screen.findByRole('button', {name: 'Mein Postfach nach oben verschieben'});
        expect(up).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Alternative nach unten verschieben'})).toBeDisabled();
        fireEvent.click(screen.getByRole('button', {name: 'Alternative nach oben verschieben'}));
        expect(reorder).toHaveBeenCalledWith(testState.identityProvider.key, [13, 12]);
        expect(screen.getByRole('button', {name: 'Mein Postfach bearbeiten'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Alternative löschen'})).toBeDisabled();
        resolveOrder([second, first]);
        await waitFor(() => expect(screen.getByRole('button', {name: 'Mein Postfach bearbeiten'})).toBeEnabled());
        const rows = within(screen.getByRole('list', {name: 'Kommunikationsanbindungen'})).getAllByRole('listitem');
        expect(rows[0]).toHaveTextContent('Alternative');
        expect(rows[1]).toHaveTextContent('Mein Postfach');
    });

    it('restores the authoritative order when saving the order fails', async () => {
        mockBindings([binding(), {...binding(), id: 13, name: 'Alternative'}]);
        vi.spyOn(CommunicationProvidersApiService.prototype, 'reorderBindings').mockRejectedValue(new Error('Conflict'));
        render(<IdentityProviderDetailsPageCommunication/>);
        fireEvent.click(await screen.findByRole('button', {name: 'Alternative nach oben verschieben'}));
        await waitFor(() => expect(CommunicationProvidersApiService.prototype.listBindings).toHaveBeenCalledTimes(2));
        const list = await screen.findByRole('list', {name: 'Kommunikationsanbindungen'});
        expect(within(list).getAllByRole('listitem')[0]).toHaveTextContent('Mein Postfach');
    });

    it('disables reordering and editing without update permission', async () => {
        testState.denied = [Permission.COMMUNICATION_PROVIDER_UPDATE];
        mockBindings([binding(), {...binding(), id: 13, name: 'Alternative'}]);
        render(<IdentityProviderDetailsPageCommunication/>);
        expect(await screen.findByRole('button', {name: 'Alternative nach oben verschieben'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Mein Postfach bearbeiten'})).toBeDisabled();
    });

    it('distinguishes loading, failures and empty lists and supports retrying', async () => {
        mockBindings([]);
        vi.mocked(CommunicationProvidersApiService.prototype.listBindings).mockRejectedValueOnce(new Error('Offline'));
        render(<IdentityProviderDetailsPageCommunication/>);
        expect(screen.getByRole('progressbar')).toBeInTheDocument();
        expect(await screen.findByRole('alert')).toHaveTextContent('Die Kommunikationsanbindungen konnten nicht geladen werden.');
        expect(screen.queryByText('Noch keine Kommunikationsanbindungen eingerichtet')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Anbindung hinzufügen'})).toBeDisabled();
        fireEvent.click(screen.getByRole('button', {name: 'Erneut versuchen'}));
        expect(await screen.findByText('Noch keine Kommunikationsanbindungen eingerichtet')).toBeInTheDocument();
    });

    it('blocks saving while the configuration could not be loaded and supports retry', async () => {
        mockBindings([binding()]);
        vi.mocked(CommunicationProvidersApiService.prototype.getBindingConfigurationLayout).mockRejectedValueOnce(new Error('Offline'));
        render(<IdentityProviderDetailsPageCommunication/>);
        fireEvent.click(await screen.findByRole('button', {name: 'Mein Postfach bearbeiten'}));
        fireEvent.change(screen.getByRole('textbox', {name: 'Anzeigename'}), {target: {value: 'Neu'}});
        expect(await screen.findByRole('alert')).toHaveTextContent('Die Konfiguration konnte nicht geladen werden.');
        expect(screen.getByRole('button', {name: 'Speichern'})).toBeDisabled();
        fireEvent.click(screen.getByRole('button', {name: 'Erneut versuchen'}));
        await waitFor(() => expect(screen.getByRole('button', {name: 'Speichern'})).toBeEnabled());
    });

    it('shows validation at the field with the matching visible name', async () => {
        mockBindings([binding()]);
        render(<IdentityProviderDetailsPageCommunication/>);
        fireEvent.click(await screen.findByRole('button', {name: 'Mein Postfach bearbeiten'}));
        fireEvent.change(screen.getByRole('textbox', {name: 'Anzeigename'}), {target: {value: ''}});
        await waitFor(() => expect(screen.getByRole('button', {name: 'Speichern'})).toBeEnabled());
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        expect(screen.getByRole('textbox', {name: 'Anzeigename'})).toHaveAccessibleDescription(expect.stringContaining('Geben Sie einen Anzeigenamen ein.'));
        expect(screen.getByRole('textbox', {name: 'Beschreibung'})).toBeInTheDocument();
    });

});

function provider(
    id: number,
    name: string,
    isEnabled: boolean,
    isTestProvider: boolean,
): CommunicationProvider {
    return {
        id,
        communicationProviderDefinitionKey: 'mail',
        communicationProviderDefinitionVersion: 1,
        name,
        description: `${name} Konfiguration`,
        configuration: {},
        isEnabled,
        isTestProvider,
    };
}
