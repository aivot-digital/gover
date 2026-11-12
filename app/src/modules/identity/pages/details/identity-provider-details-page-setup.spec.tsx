import {describe, expect, it} from 'vitest';
import {createSamlMetadataUrl} from './identity-provider-details-page-setup';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import {IdentityProviderDetailsDTO} from '../../models/identity-provider-details-dto';

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
