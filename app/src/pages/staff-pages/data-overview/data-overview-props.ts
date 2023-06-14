import {IconDefinition} from "@fortawesome/pro-duotone-svg-icons";

export interface DataOverviewProps<T extends { id: number }> {
    title: string;
    searchPlaceholder: string;
    addLabel: string;
    noItemsHelperText: string;
    emptySearchHelperText: string;

    exportExtension: string;

    list: () => Promise<T[]>;
    create: () => Promise<T>;
    import: (data: Omit<T, 'id'>) => Promise<T>;
    update: (updated: T) => Promise<T>;
    destroy: (destroyed: T) => Promise<void>;
    search: (search: string, item: T) => boolean;
    sort: (itemA: T, itemB: T) => number;
    toPrimaryString: (item: T) => string;
    toSecondaryString?: (item: T) => string | undefined;

    linkToEdit?: (item: T) => string;
    fieldsToEdit?: (EditField<T> | string)[];

    additionalActions?: {
        label: string;
        icon: IconDefinition;
        key: string;
    }[];
    onAdditionalAction?: (key: string, item: T) => void;
}

export interface EditField<T> {
    field: keyof T;
    label: string;
    helperText?: string;
    placeholder: string;
    isRichtext?: boolean;
    isMultiline?: boolean;
    isOptions?: boolean;
    options?: string[];
    showIf?: (item: T) => boolean;
    required?: boolean;
}
