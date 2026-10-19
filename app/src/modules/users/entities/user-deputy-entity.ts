import {LocalDateIso} from '../../../utils/temporal-types';

export interface UserDeputyEntity {
    id: number;
    originalUserId: string;
    deputyUserId: string;
    fromDate: LocalDateIso;
    untilDate: LocalDateIso | null;
}
