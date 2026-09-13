import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {CommunicationProvidersApiService} from '../../../communication/communication-providers-api-service';
import {type CommunicationProvider, type CommunicationProviderDefinition} from '../../../communication/models';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import {IdentityProviderDetailsPageCommunication} from './identity-provider-details-page-communication';

const testState = vi.hoisted(() => ({
    dispatch: vi.fn(),
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
    useHasSystemPermission: () => true,
}));

describe('IdentityProviderDetailsPageCommunication', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        testState.dispatch.mockReset();
        testState.identityProvider = {
            key: '00000000-0000-0000-0000-000000000001',
            type: IdentityProviderType.BundID,
            isTestProvider: true,
        };
    });

    it('offers supported providers regardless of test and activity status', async () => {
        const providers: CommunicationProvider[] = [
            provider(7, 'Produktiver Anbieter', true, false),
            provider(8, 'Inaktiver Testanbieter', false, true),
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

        const addButton = screen.getByRole('button', {name: 'Anbieter hinzufügen'});
        await waitFor(() => expect(addButton).toBeEnabled());
        fireEvent.click(addButton);
        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Kommunikationsanbieter'}));

        expect(await screen.findByText('Produktiver Anbieter')).toBeInTheDocument();
        expect(screen.getByText('Inaktiver Testanbieter')).toBeInTheDocument();
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
