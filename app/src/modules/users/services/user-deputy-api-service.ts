import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {UserDeputyEntity} from "../entities/user-deputy-entity";

export interface UserDeputyFilter {
    originalUserId: string;
    deputyUserId: boolean;
    fromTimestamp: string;
    untilTimestampIsNull: boolean;
}

export class UserDeputyApiService extends BaseCrudApiService<
    UserDeputyEntity,
    UserDeputyEntity,
    UserDeputyEntity,
    UserDeputyEntity,
    number,
    UserDeputyFilter
> {
    public constructor() {
        super('/api/user-deputies/');
    }

    public initialize(): UserDeputyEntity {
        return {
            deputyUserId: "",
            fromTimestamp: "",
            id: 0,
            originalUserId: new Date().toISOString(),
            untilTimestamp: null,
        };
    }
}