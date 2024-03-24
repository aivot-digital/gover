import {PropsWithChildren} from 'react';

export interface StatusTableProps {
    label: string;
    items: PropsWithChildren<{
        label: string;
        icon: JSX.Element;
    }>[];
}