import {type ReactNode} from 'react';

export interface SelectFieldComponentOption {
    label: string;
    subLabel?: string;
    icon?: ReactNode;
    value: string;
}
