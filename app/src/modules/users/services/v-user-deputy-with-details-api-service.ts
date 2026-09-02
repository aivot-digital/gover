import {BaseCrudApiService} from "../../../services/base-crud-api-service";
import {VUserDeputyWithDetailsEntity} from "../entities/v-user-deputy-with-details-entity";
import {LocalDateIso} from '../../../utils/temporal-types';
import {getCurrentApplicationDate} from '../../../utils/temporal-utils';

export interface VUserDeputyWithDetailsFilter {
    originalUserId: string;
    originalUserFullName: string;
    deputyUserId: string;
    deputyUserFullName: string;
    fromDate: LocalDateIso;
    untilDateIsNull: boolean;
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
            deputyUserId: "",
            deputyUserDeletedInIdp: false,
            deputyUserEmail: "",
            deputyUserEnabled: false,
            deputyUserFirstName: "",
            deputyUserFullName: "",
            deputyUserLastName: "",
            deputyUserSystemRoleId: 0,
            deputyUserVerified: false,
            fromDate: getCurrentApplicationDate(),
            id: 0,
            originalUserId: "",
            originalUserDeletedInIdp: false,
            originalUserEmail: "",
            originalUserEnabled: false,
            originalUserFirstName: "",
            originalUserFullName: "",
            originalUserLastName: "",
            originalUserSystemRoleId: 0,
            originalUserVerified: false,
            untilDate: null,
            active: false,
        };
    }
}
