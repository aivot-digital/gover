import {ReactNode} from 'react';

export interface SearchBaseDialogTabProps<T> {
    title: string;
    options: T[];
    onSelect: (option: T) => void;
    searchPlaceholder: string;
    searchKeys: (keyof T)[];
    primaryTextKey: keyof T | ((option: T) => string);
    secondaryTextKey?: keyof T | ((option: T) => string);
    getId: keyof T | ((option: T) => string);
    getIcon?: (option: T) => ReactNode;
    detailsBuilder?: (option: T) => ReactNode;
    noSearchResultsMessage?: ReactNode;
    noOptionsMessage?: ReactNode;
}