import {format} from "date-fns/format";

export function formatISODate(isoString: string): string {
    if (isoString.endsWith('Z')) {
        return format(isoString, 'dd.MM.yyyy');
    }
    return format(isoString + 'Z', 'dd.MM.yyyy');
}