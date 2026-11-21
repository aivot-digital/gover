import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {createSamlMetadataUrl, IdentityProviderDetailsPageSetup} from './identity-provider-details-page-setup';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import {IdentityProviderDetailsDTO} from '../../models/identity-provider-details-dto';

vi.mock('react-redux', async (importOriginal) => ({
    ...await importOriginal<typeof import('react-redux')>(),
    useDispatch: () => vi.fn(),
}));

// Shape of the broker descriptor Keycloak publishes for a SAML identity provider.
const KEYCLOAK_BROKER_DESCRIPTOR = `<md:EntityDescriptor xmlns="urn:oasis:names:tc:SAML:2.0:metadata" xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" entityID="https://login.example.org/realms/customer">
    <md:SPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
        <md:KeyDescriptor use="signing"><ds:KeyInfo><ds:X509Data><ds:X509Certificate>SIGNING</ds:X509Certificate></ds:X509Data></ds:KeyInfo></md:KeyDescriptor>
        <md:KeyDescriptor use="encryption"><ds:KeyInfo><ds:X509Data><ds:X509Certificate>ENCRYPTION</ds:X509Certificate></ds:X509Data></ds:KeyInfo></md:KeyDescriptor>
        <md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://login.example.org/realms/customer/broker/bundid/endpoint" index="1"/>
    </md:SPSSODescriptor>
</md:EntityDescriptor>`;

function createProvider(
    type: IdentityProviderType,
    authorizationEndpoint: string,
    idpHint?: string,
): IdentityProviderDetailsDTO {
    return {
        type,
        authorizationEndpoint,
        additionalParams: idpHint == null ? [] : [{key: 'kc_idp_hint', value: idpHint}],
    } as IdentityProviderDetailsDTO;
}

describe('createSamlMetadataUrl', () => {
    it('uses the Keycloak host and realm of the provider endpoint for brokered providers', () => {
        const provider = createProvider(
            IdentityProviderType.BundID,
            'https://login.example.org/auth/realms/citizens/protocol/openid-connect/auth',
            'bundid',
        );

        expect(createSamlMetadataUrl(provider))
            .toBe('https://login.example.org/auth/realms/citizens/broker/bundid/endpoint/descriptor');
    });

    it('keeps relative endpoints relative', () => {
        const provider = createProvider(
            IdentityProviderType.MUK,
            '/idp/realms/customer/protocol/openid-connect/auth',
            'muk',
        );

        expect(createSamlMetadataUrl(provider)).toBe('/idp/realms/customer/broker/muk/endpoint/descriptor');
    });

    it('uses the BayernID metadata endpoint for BayernID', () => {
        const provider = createProvider(
            IdentityProviderType.BayernID,
            'https://login.example.org/realms/citizens/protocol/openid-connect/auth',
            'bayernid',
        );

        expect(createSamlMetadataUrl(provider)).toBe('https://login.example.org/realms/citizens/bayernid/bayernid/metadata');
    });

    it('returns no URL without an identity provider hint', () => {
        const provider = createProvider(
            IdentityProviderType.SHID,
            '/idp/realms/customer/protocol/openid-connect/auth',
        );

        expect(createSamlMetadataUrl(provider)).toBeUndefined();
    });

    it('returns no URL when the endpoint is not a Keycloak realm endpoint', () => {
        const provider = createProvider(
            IdentityProviderType.BundID,
            'https://login.example.org/oauth2/authorize',
            'bundid',
        );

        expect(createSamlMetadataUrl(provider)).toBeUndefined();
    });
});

describe('IdentityProviderDetailsPageSetup', () => {
    it('loads the setup metadata through the backend instead of requesting Keycloak from the browser', async () => {
        const provider = {
            ...createProvider(
                IdentityProviderType.BundID,
                'https://login.example.org/realms/customer/protocol/openid-connect/auth',
                'bundid',
            ),
            key: 'provider-key',
        };
        const getSamlMetadata = vi.spyOn(IdentityProvidersApiService.prototype, 'getSamlMetadata')
            .mockResolvedValue(KEYCLOAK_BROKER_DESCRIPTOR);
        const browserFetch = vi.spyOn(globalThis, 'fetch');

        render(
            <GenericDetailsPageContext.Provider value={{item: provider} as never}>
                <IdentityProviderDetailsPageSetup/>
            </GenericDetailsPageContext.Provider>,
        );
        fireEvent.click(screen.getByRole('button', {name: 'Metadaten für Einrichtung auslesen'}));

        expect(await screen.findByDisplayValue('https://login.example.org/realms/customer')).toBeTruthy();
        expect(screen.getByDisplayValue('SIGNING')).toBeTruthy();
        expect(screen.getByDisplayValue('ENCRYPTION')).toBeTruthy();
        expect(screen.getByDisplayValue('https://login.example.org/realms/customer/broker/bundid/endpoint')).toBeTruthy();
        expect(getSamlMetadata).toHaveBeenCalledWith('provider-key');
        expect(browserFetch).not.toHaveBeenCalled();
    });
});
