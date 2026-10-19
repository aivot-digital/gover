import {dateValueToDateTime} from './temporal-utils';

export function formatLocalDate(value: string): string {
    return dateValueToDateTime(value, 'day')?.toFormat('dd.MM.yyyy') ?? value;
}
