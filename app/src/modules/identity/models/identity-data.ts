export interface IdentityData {
    identityId: string;
    providerKey: string;
    metadataIdentifier: string;
    attributes: Record<string, string>;
}