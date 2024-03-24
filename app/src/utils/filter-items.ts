import {isStringNullOrEmpty} from './string-utils';

export function filterItems<T>(items: T[] | null | undefined, field: (keyof T) | Array<keyof T>, search: string): T[] {
    if (items == null) {
        return [];
    }

    if (isStringNullOrEmpty(search)) {
        return items;
    }

    return items.filter((item) => {
        if (Array.isArray(field)) {
            for (const fieldItem of field) {
                const value: any = item[fieldItem];

                if (value == null) {
                    continue;
                }

                if (typeof value === 'string') {
                    if (value.toLowerCase().includes(search.toLowerCase())) {
                        return true;
                    }
                }
            }
        } else {
            const value: any = item[field];

            if (value == null) {
                return false;
            }

            if (typeof value === 'string') {
                return value.toLowerCase().includes(search.toLowerCase());
            }

            return false;
        }
    });
}
