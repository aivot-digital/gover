import {isNullOrEmpty} from './is-null-or-empty';

export function stringOrDefault(val: string | null | undefined, def: string): string {
    if (isNullOrEmpty(val)) {
        return def;
    }
    return val!;
}