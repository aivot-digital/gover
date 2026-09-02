export interface IdentityData {
    sessionId: string;
    identityId: string;
    providerKey: string;
    metadataIdentifier: string;
    attributes: Record<string, string>;
}

export type IdentityDataMap = Record<string, IdentityData>;