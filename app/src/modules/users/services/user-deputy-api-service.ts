import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {UserDeputyEntity} from "../entities/user-deputy-entity";
import {LocalDateIso} from '../../../utils/temporal-types';
import {getCurrentApplicationDate} from '../../../utils/temporal-utils';

export interface UserDeputyFilter {
    originalUserId: string;
    deputyUserId: string;
    fromDate: LocalDateIso;
    untilDateIsNull: boolean;
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
            fromDate: getCurrentApplicationDate(),
            id: 0,
            originalUserId: "",
            untilDate: null,
        };
    }
}
