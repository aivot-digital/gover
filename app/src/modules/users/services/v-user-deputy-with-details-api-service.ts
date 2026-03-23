import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {VUserDeputyWithDetailsEntity} from "../entities/v-user-deputy-with-details-entity";

export interface VUserDeputyWithDetailsFilter {
    originalUserId: string;
    originalUserFullName: string;
    deputyUserId: boolean;
    deputyUserFullName: string;
    fromTimestamp: string;
    untilTimestampIsNull: boolean;
}

export class VUserDeputyWithDetailsApiService extends BaseCrudApiService<
    VUserDeputyWithDetailsEntity,
    VUserDeputyWithDetailsEntity,
    VUserDeputyWithDetailsEntity,
    VUserDeputyWithDetailsEntity,
    number,
    VUserDeputyWithDetailsFilter
> {
    public constructor() {
        super('/api/user-deputies-with-details/');
    }

    public initialize(): VUserDeputyWithDetailsEntity {
        return {
            deputyUserDeletedInIdp: false,
            deputyUserEmail: "",
            deputyUserEnabled: false,
            deputyUserFirstName: "",
            deputyUserFullName: "",
            deputyUserLastName: "",
            deputyUserSystemRoleId: 0,
            deputyUserVerified: false,
            fromTimestamp: new Date().toISOString(),
            id: 0,
            originalUserDeletedInIdp: false,
            originalUserEmail: "",
            originalUserEnabled: false,
            originalUserFirstName: "",
            originalUserFullName: "",
            originalUserLastName: "",
            originalUserSystemRoleId: 0,
            originalUserVerified: false,
            untilTimestamp: null,
            active: false,
        };
    }
}