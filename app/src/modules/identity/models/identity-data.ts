export interface IdentityData {
    sessionId: string;
    identityId: string;
    type: 'IdentityProvider' | 'Email';
    providerKey: string | null;
    metadataIdentifier: string | null;
    emailAddress: string | null;
    attributes: Record<string, string>;
    communicationProviderBindingId: number | null;
    communicationProviderData: Record<string, unknown>;
}

export type IdentityDataMap = Record<string, IdentityData>;
