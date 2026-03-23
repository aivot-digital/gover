export interface VUserDeputyWithDetailsEntity {
    id: number;
    fromTimestamp: string;
    untilTimestamp: string | null;
    active: boolean;

    originalUserEmail: string;
    originalUserFirstName: string;
    originalUserLastName: string;
    originalUserEnabled: boolean;
    originalUserVerified: boolean;
    originalUserDeletedInIdp: boolean;
    originalUserSystemRoleId: number;
    originalUserFullName: string;

    deputyUserEmail: string;
    deputyUserFirstName: string;
    deputyUserLastName: string;
    deputyUserEnabled: boolean;
    deputyUserVerified: boolean;
    deputyUserDeletedInIdp: boolean;
    deputyUserSystemRoleId: number;
    deputyUserFullName: string;
}