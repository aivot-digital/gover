import {render, screen, waitFor, within} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {IdentityProvidersApiService} from '../../identity/identity-providers-api-service';
import {IdentityProviderType} from '../../identity/enums/identity-provider-type';
import {type IdentityAttributeMapping} from '../../identity/models/identity-attribute-mapping';
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
    uniqueIdFromIdentityProvider: 'provider-user-123',
    emailAddress: null,
    attributes: {
        family_name: 'Muster',
        familyName: 'Duplicate family name',
        given_name: 'Erika',
        givenName: 'Duplicate given name',
        trust_level_authentication: 'level4',
        unmapped_claim: 'Unmapped value',
    },
    communicationProviderBindingId: 17,
    communicationProviderData: {
        mailbox: 'internal-mailbox-reference',
    },
};

const identityProviderAttributes: IdentityAttributeMapping[] = [
    {
        label: 'Nachname',
        description: 'Familienname der Person',
        keyInData: 'family_name',
        displayAttribute: false,
    },
    {
        label: 'Vorname',
        description: 'Vorname der Person',
        keyInData: 'given_name',
        displayAttribute: false,
    },
    {
        label: 'Geburtsdatum',
        description: 'Geburtsdatum der Person',
        keyInData: 'date_of_birth',
        displayAttribute: false,
    },
    {
        label: 'Vertrauensniveau',
        description: 'Qualitätsstufe der Authentifizierung',
        keyInData: 'trust_level_authentication',
        displayAttribute: true,
    },
];

const emailIdentity: IdentityData = {
    sessionId: 'internal-email-session-id',
    identityId: 'contact',
    type: 'Email',
    providerKey: null,
    metadataIdentifier: null,
    uniqueIdFromIdentityProvider: null,
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

function mockIdentityProviders(
    attributes: IdentityAttributeMapping[] = identityProviderAttributes,
    type: IdentityProviderType = IdentityProviderType.BundID,
) {
    return vi.spyOn(IdentityProvidersApiService.prototype, 'listAll').mockResolvedValue({
        content: [{
            key: identityProviderKey,
            metadataIdentifier: 'urn:bundid:metadata',
            type,
            name: 'BundID Produktion',
            description: 'Interne Beschreibung des Nutzerkontenanbieters',
            iconAssetKey: null,
            attributes,
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
        expect(screen.queryByText(identityProviderKey)).not.toBeInTheDocument();
        expect(screen.queryByText('urn:bundid:metadata')).not.toBeInTheDocument();
        expect(screen.queryByText('17')).not.toBeInTheDocument();
        expect(screen.queryByText('8')).not.toBeInTheDocument();
        expect(screen.getByText('Nachname')).toBeInTheDocument();
        expect(screen.getByText('Vorname')).toBeInTheDocument();
        expect(screen.getByText('Geburtsdatum')).toBeInTheDocument();
        expect(screen.getByText('Muster')).toBeInTheDocument();
        expect(screen.getByText('Erika')).toBeInTheDocument();
        expect(screen.getByText('Vertrauensniveau')).toBeInTheDocument();
        expect(screen.getByText('level4')).toBeInTheDocument();
        expect(screen.getByText('Kein Wert übergeben')).toBeInTheDocument();

        expect(screen.queryByText('family_name')).not.toBeInTheDocument();
        expect(screen.queryByText('familyName')).not.toBeInTheDocument();
        expect(screen.queryByText('Duplicate family name')).not.toBeInTheDocument();
        expect(screen.queryByText('givenName')).not.toBeInTheDocument();
        expect(screen.queryByText('Duplicate given name')).not.toBeInTheDocument();
        expect(screen.queryByText('unmapped_claim')).not.toBeInTheDocument();
        expect(screen.queryByText('Unmapped value')).not.toBeInTheDocument();

        expect(screen.queryByText('internal-session-id')).not.toBeInTheDocument();
        expect(screen.queryByText('internal-mailbox-reference')).not.toBeInTheDocument();
        expect(screen.queryByText('internal-provider-configuration')).not.toBeInTheDocument();
        expect(screen.queryByText('internal-binding-configuration')).not.toBeInTheDocument();

        expect(listIdentityProviders).toHaveBeenCalledWith({keys: [identityProviderKey]});
        expect(listProviders).toHaveBeenCalledTimes(1);
        expect(listBindings).toHaveBeenCalledWith(identityProviderKey);
    });

    it.each([
        IdentityProviderType.BayernID,
        IdentityProviderType.BundID,
        IdentityProviderType.SHID,
    ])('shows the trust level once as provider detail for %s', async (type) => {
        mockIdentityProviders(identityProviderAttributes, type);
        mockCommunicationProviders();

        renderList({applicant: providerIdentity});

        await screen.findByText('BundID Produktion');
        const article = screen.getByRole('article', {name: 'Identität applicant'});
        const attributeTable = within(article).getByRole('table', {name: 'Attribute der Identität applicant'});
        expect(within(article).getAllByText('Vertrauensniveau')).toHaveLength(1);
        expect(within(article).getByText('level4')).toBeInTheDocument();
        expect(within(attributeTable).queryByText('Vertrauensniveau')).not.toBeInTheDocument();
        expect(within(attributeTable).queryByText('level4')).not.toBeInTheDocument();
    });

    it('shows a missing trust level for a supported provider', async () => {
        mockIdentityProviders();
        mockCommunicationProviders();
        const identityWithoutTrustLevel: IdentityData = {
            ...providerIdentity,
            attributes: {...providerIdentity.attributes},
        };
        delete identityWithoutTrustLevel.attributes.trust_level_authentication;

        renderList({applicant: identityWithoutTrustLevel});

        await screen.findByText('BundID Produktion');
        expect(screen.getByText('Vertrauensniveau').closest('div'))
            .toHaveTextContent('VertrauensniveauKein Wert übergeben');
    });

    it.each([
        IdentityProviderType.Custom,
        IdentityProviderType.MUK,
    ])('does not show an explicit trust level for %s', async (type) => {
        mockIdentityProviders(identityProviderAttributes, type);
        mockCommunicationProviders();

        renderList({applicant: providerIdentity});

        await screen.findByText('BundID Produktion');
        const article = screen.getByRole('article', {name: 'Identität applicant'});
        const attributeTable = within(article).getByRole('table', {name: 'Attribute der Identität applicant'});
        expect(within(article).getAllByText('Vertrauensniveau')).toHaveLength(1);
        expect(within(attributeTable).getByText('Vertrauensniveau')).toBeInTheDocument();
        expect(within(attributeTable).getByText('level4')).toBeInTheDocument();
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

        expect(screen.queryByText(identityProviderKey)).not.toBeInTheDocument();
        expect(screen.queryByText('17')).not.toBeInTheDocument();
        expect(screen.getAllByText('Name mangels Berechtigung nicht verfügbar').length).toBeGreaterThan(0);
        expect(screen.getByText('Über die Anbindung nicht auflösbar')).toBeInTheDocument();
        expect(screen.getByText('Attributzuweisungen mangels Berechtigung nicht verfügbar')).toBeInTheDocument();
        expect(screen.queryByText('Muster')).not.toBeInTheDocument();
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
        expect(screen.queryByText('8')).not.toBeInTheDocument();
        expect(screen.getByText('Name nicht verfügbar')).toBeInTheDocument();
        expect(screen.getByText('BundID Produktion')).toBeInTheDocument();
    });

    it('does not expose raw attributes when the identity-provider lookup fails', async () => {
        vi.spyOn(IdentityProvidersApiService.prototype, 'listAll')
            .mockRejectedValue(new Error('network'));
        mockCommunicationProviders();

        renderList({applicant: providerIdentity});

        expect(await screen.findByText('Attributzuweisungen nicht verfügbar')).toBeInTheDocument();
        expect(screen.queryByText('Muster')).not.toBeInTheDocument();
        expect(screen.queryByText('Duplicate family name')).not.toBeInTheDocument();
        expect(screen.queryByText('Unmapped value')).not.toBeInTheDocument();
    });

    it('sorts identities and preserves mapping order while deduplicating provider requests', async () => {
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
                given_name: 'First',
                family_name: 'Last',
            },
        };

        renderList({zeta: secondIdentity, alpha: firstIdentity});

        await screen.findAllByText('BundID Produktion');
        const cards = screen.getAllByRole('article');
        expect(within(cards[0]).getByRole('heading', {name: 'alpha'})).toBeInTheDocument();
        expect(within(cards[1]).getByRole('heading', {name: 'zeta'})).toBeInTheDocument();

        const attributeRows = within(cards[0]).getAllByRole('row');
        expect(attributeRows[1]).toHaveTextContent('NachnameLast');
        expect(attributeRows[2]).toHaveTextContent('VornameFirst');
        expect(attributeRows[3]).toHaveTextContent('GeburtsdatumKein Wert übergeben');
        expect(within(cards[1]).getAllByText('Kein Wert übergeben')).toHaveLength(4);
        expect(listIdentityProviders).toHaveBeenCalledTimes(1);
        expect(listBindings).toHaveBeenCalledTimes(1);
    });

    it('shows an empty configuration instead of raw provider attributes', async () => {
        mockIdentityProviders([]);
        mockCommunicationProviders();

        renderList({applicant: providerIdentity});

        expect(await screen.findByText('Keine Attribute konfiguriert')).toBeInTheDocument();
        expect(screen.queryByText('Muster')).not.toBeInTheDocument();
    });

    it('renders nothing for an empty identity map', async () => {
        const {container} = renderList({});

        expect(container).toBeEmptyDOMElement();
        await waitFor(() => {
            expect(screen.queryByRole('article')).not.toBeInTheDocument();
        });
    });
});
