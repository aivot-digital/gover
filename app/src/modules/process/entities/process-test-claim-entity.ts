export interface ProcessTestClaimEntity {
    id: number;
    accessKey: string;
    processTestConfigId?: number;
    processId: number;
    processVersion: number;
    created: string; // ISO date string
    owningUserId: string;
}
