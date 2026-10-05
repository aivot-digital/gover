import {type ReactNode} from 'react';

export type SelectFieldValue = string | number;

export interface SelectFieldComponentOption<T extends SelectFieldValue = string> {
    label: string;
    subLabel?: string;
    icon?: ReactNode;
    value: T;
}
