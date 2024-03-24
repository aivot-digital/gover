import {format as dateFormat, parseISO} from 'date-fns';

export function formatIsoDate(date?: string, format?: string): string {
    if (date == null) {
        return 'Ungültiges Datum';
    }

    const d = parseISO(date);

    return dateFormat(d, format ?? 'dd.MM.yyyy');
}
