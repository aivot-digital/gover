export function filterItems<T> (items: T[] | null | undefined, field: keyof T, search: string): T[] {
    if (items == null) {
        return [];
    }

    return items.filter(item => {
        const value = item[field];

        if (typeof value === 'string') {
            return value.toLowerCase().includes(search.toLowerCase());
        }

        return false;
    });
}
