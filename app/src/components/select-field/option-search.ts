import {createFilterOptions} from '@mui/material/Autocomplete';

/** Callers choose searchable metadata; free text and the selected values are never normalized here. */
export function createOptionFilter<T>(getParts: (option: T) => Array<string | null | undefined>) {
    return createFilterOptions<T>({
        trim: true,
        stringify: (option) => getParts(option).filter(Boolean).join(' '),
    });
}
