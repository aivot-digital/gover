export interface UserDeputyEntity {
    id: number;
    originalUserId: string;
    deputyUserId: string;
    fromTimestamp: string;
    untilTimestamp: string | null;
}