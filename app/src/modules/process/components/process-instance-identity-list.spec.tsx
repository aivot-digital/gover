import {render, screen, waitFor, within} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {IdentityProvidersApiService} from '../../identity/identity-providers-api-service';
import {IdentityProviderType} from '../../identity/enums/identity-provider-type';
import {type IdentityData, type IdentityDataMap} from '../../identity/models/identity-data';
import {CommunicationProvidersApiService} from '../../communication/communication-providers-api-service';
import {type CommunicationProvider, type CommunicationProviderBinding} from '../../communication/models';
import {ProcessInstanceIdentityList} from './process-instance-identity-list';

const identityProviderKey = '36a9a19d-f9fb-4225-a9a0-07a223820b4b';

const providerIdentity: IdentityData = {
    sessionId: 'internal-session-id',
    identityId: 'applicant',
    type: 'IdentityProvider',
    providerKey: identityProviderKey,
    metadataIdentifier: 'urn:bundid:metadata',
    emailAddress: null,
    attributes: {
        family_name: 'Muster',
        given_name: 'Erika',
    },
    communicationProviderBindingId: 17,
    communicationProviderData: {
        mailbox: 'internal-mailbox-reference',
    },
};

const emailIdentity: IdentityData = {
    sessionId: 'internal-email-session-id',
    identityId: 'contact',
    type: 'Email',
    providerKey: null,
    metadataIdentifier: null,
    emailAddress: 'person@example.org',
    attributes: {
        email: 'person@example.org',
    },
    communicationProviderBindingId: null,
    communicationProviderData: {},
};

const communicationProvider: CommunicationProvider = {
    id: 8,
    communicationProviderDefinitionKey: 'mail',
    communicationProviderDefinitionVersion: 1,
    name: 'Behördenpostfach',
    description: 'Interne Beschreibung des Kommunikationsanbieters',
    configuration: {
        secret: 'internal-provider-configuration',
    },
    isEnabled: true,
    isTestProvider: false,
};

const communicationBinding: CommunicationProviderBinding = {
    id: 17,
    identityProviderKey,
    communicationProviderId: communicationProvider.id,
    name: 'BundID-Postfach',
    description: 'Interne Beschreibung der Anbindung',
    isEnabled: true,
    position: 0,
    configuration: {
        secret: 'internal-binding-configuration',
    },
};

function mockIdentityProviders() {
    return vi.spyOn(IdentityProvidersApiService.prototype, 'listAll').mockResolvedValue({
        content: [{
            key: identityProviderKey,
            metadataIdentifier: 'urn:bundid:metadata',
            type: IdentityProviderType.BundID,
            name: 'BundID Produktion',
            description: 'Interne Beschreibung des Nutzerkontenanbieters',
            iconAssetKey: null,
            attributes: [],
            isEnabled: true,
            isTestProvider: false,
        }],
        page: {
            size: 999,
            number: 0,
            totalElements: 1,
            totalPages: 1,
        },
    });
}

function mockCommunicationProviders() {
    const listProviders = vi.spyOn(CommunicationProvidersApiService.prototype, 'listProviders')
        .mockResolvedValue([communicationProvider]);
    const listBindings = vi.spyOn(CommunicationProvidersApiService.prototype, 'listBindings')
        .mockResolvedValue([communicationBinding]);

    return {listProviders, listBindings};
}

function renderList(
    identities: IdentityDataMap,
    canReadIdentityProviders = true,
    canReadCommunicationProviders = true,
) {
    return render(
        <ProcessInstanceIdentityList
            identities={identities}
            canReadIdentityProviders={canReadIdentityProviders}
            canReadCommunicationProviders={canReadCommunicationProviders}
            title={null}
        />,
    );
}

describe('ProcessInstanceIdentityList', () => {
    afterEach(() => vi.restoreAllMocks());

    it('resolves and displays the selected identity provider, binding, and communication provider', async () => {
        const listIdentityProviders = mockIdentityProviders();
        const {listProviders, listBindings} = mockCommunicationProviders();

        renderList({applicant: providerIdentity});

        expect(await screen.findByText('BundID Produktion')).toBeInTheDocument();
        expect(screen.getByText('BundID-Postfach')).toBeInTheDocument();
        expect(screen.getByText('Behördenpostfach')).toBeInTheDocument();
        expect(screen.getByText(identityProviderKey)).toBeInTheDocument();
        expect(screen.getByText('urn:bundid:metadata')).toBeInTheDocument();
        expect(screen.getByText('17')).toBeInTheDocument();
        expect(screen.getByText('8')).toBeInTheDocument();
        expect(screen.getByText('family_name')).toBeInTheDocument();
        expect(screen.getByText('Muster')).toBeInTheDocument();

        expect(screen.queryByText('internal-session-id')).not.toBeInTheDocument();
        expect(screen.queryByText('internal-mailbox-reference')).not.toBeInTheDocument();
        expect(screen.queryByText('internal-provider-configuration')).not.toBeInTheDocument();
        expect(screen.queryByText('internal-binding-configuration')).not.toBeInTheDocument();

        expect(listIdentityProviders).toHaveBeenCalledWith({keys: [identityProviderKey]});
        expect(listProviders).toHaveBeenCalledTimes(1);
        expect(listBindings).toHaveBeenCalledWith(identityProviderKey);
    });

    it('shows direct email identities without requesting provider catalogs', () => {
        const listIdentityProviders = vi.spyOn(IdentityProvidersApiService.prototype, 'listAll');
        const listProviders = vi.spyOn(CommunicationProvidersApiService.prototype, 'listProviders');
        const listBindings = vi.spyOn(CommunicationProvidersApiService.prototype, 'listBindings');

        renderList({contact: emailIdentity});

        expect(screen.getByText('E-Mail-Identität')).toBeInTheDocument();
        expect(screen.getAllByText('person@example.org')).toHaveLength(2);
        expect(screen.getByText('Nicht zutreffend – direkte E-Mail-Identität')).toBeInTheDocument();
        expect(screen.getByText('Nicht zutreffend – direkter E-Mail-Versand')).toBeInTheDocument();
        expect(listIdentityProviders).not.toHaveBeenCalled();
        expect(listProviders).not.toHaveBeenCalled();
        expect(listBindings).not.toHaveBeenCalled();
    });

    it('uses stored references without making unauthorized provider requests', () => {
        const listIdentityProviders = vi.spyOn(IdentityProvidersApiService.prototype, 'listAll');
        const listProviders = vi.spyOn(CommunicationProvidersApiService.prototype, 'listProviders');
        const listBindings = vi.spyOn(CommunicationProvidersApiService.prototype, 'listBindings');

        renderList({applicant: providerIdentity}, false, false);

        expect(screen.getByText(identityProviderKey)).toBeInTheDocument();
        expect(screen.getByText('17')).toBeInTheDocument();
        expect(screen.getAllByText('Name mangels Berechtigung nicht verfügbar').length).toBeGreaterThan(0);
        expect(screen.getByText('Über die Anbindung nicht auflösbar')).toBeInTheDocument();
        expect(listIdentityProviders).not.toHaveBeenCalled();
        expect(listProviders).not.toHaveBeenCalled();
        expect(listBindings).not.toHaveBeenCalled();
    });

    it('keeps successful binding details when the communication-provider catalog fails', async () => {
        mockIdentityProviders();
        vi.spyOn(CommunicationProvidersApiService.prototype, 'listProviders')
            .mockRejectedValue(new Error('network'));
        vi.spyOn(CommunicationProvidersApiService.prototype, 'listBindings')
            .mockResolvedValue([communicationBinding]);

        renderList({applicant: providerIdentity});

        expect(await screen.findByText('BundID-Postfach')).toBeInTheDocument();
        expect(screen.getByText('8')).toBeInTheDocument();
        expect(screen.getByText('Name nicht verfügbar')).toBeInTheDocument();
        expect(screen.getByText('BundID Produktion')).toBeInTheDocument();
    });

    it('sorts identities and attributes while deduplicating provider requests', async () => {
        const listIdentityProviders = mockIdentityProviders();
        const {listBindings} = mockCommunicationProviders();
        const secondIdentity: IdentityData = {
            ...providerIdentity,
            identityId: 'zeta',
            attributes: {},
        };
        const firstIdentity: IdentityData = {
            ...providerIdentity,
            identityId: 'alpha',
            attributes: {
                zeta: 'last',
                alpha: 'first',
            },
        };

        renderList({zeta: secondIdentity, alpha: firstIdentity});

        await screen.findAllByText('BundID Produktion');
        const cards = screen.getAllByRole('article');
        expect(within(cards[0]).getByRole('heading', {name: 'alpha'})).toBeInTheDocument();
        expect(within(cards[1]).getByRole('heading', {name: 'zeta'})).toBeInTheDocument();

        const attributeRows = within(cards[0]).getAllByRole('row');
        expect(attributeRows[1]).toHaveTextContent('alphafirst');
        expect(attributeRows[2]).toHaveTextContent('zetalast');
        expect(within(cards[1]).getByText('Keine Attribute vorhanden')).toBeInTheDocument();
        expect(listIdentityProviders).toHaveBeenCalledTimes(1);
        expect(listBindings).toHaveBeenCalledTimes(1);
    });

    it('renders nothing for an empty identity map', async () => {
        const {container} = renderList({});

        expect(container).toBeEmptyDOMElement();
        await waitFor(() => {
            expect(screen.queryByRole('article')).not.toBeInTheDocument();
        });
    });
});
