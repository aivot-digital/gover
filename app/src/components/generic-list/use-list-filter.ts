import {useCallback, useMemo} from 'react';
import {useSearchParams} from 'react-router-dom';

/** Keep additional filters and pagination in one URL update, including browser back/forward navigation. */
export function useListFilter(key: string) {
    const [searchParams, setSearchParams] = useSearchParams();
    const values = useMemo(() => searchParams.getAll(key).filter(value => value.trim() !== ''), [key, searchParams]);
    const setValue = useCallback((value: string | readonly string[] | null | undefined) => {
        setSearchParams(current => {
            const next = new URLSearchParams(current);
            next.delete(key);
            const values = typeof value === 'string' ? [value] : value ?? [];
            values.filter(entry => entry.trim() !== '').forEach(entry => next.append(key, entry));
            next.set('page', '1');
            return next;
        });
    }, [key, setSearchParams]);

    return {value: values[0], values, setValue};
}
