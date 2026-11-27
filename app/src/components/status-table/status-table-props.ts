import {PropsWithChildren} from 'react';
import {SxProps, TypographyVariant} from '@mui/material';

export type StatusTablePropsItem = PropsWithChildren<{
    label: string;
    icon?: React.ReactNode;
    alignTop?: boolean;
    subItems?: StatusTablePropsItem[] | null;
    details?: React.ReactNode;
    detailsLabel?: string;
    detailsExpandedLabel?: string;
    detailsDefaultExpanded?: boolean;
    indentDetails?: boolean;
}>;

export interface StatusTableProps {
    sx?: SxProps;
    label?: string;
    labelVariant?: TypographyVariant;
    labelSx?: SxProps;
    description?: string;
    descriptionSx?: SxProps;
    cardSx?: SxProps;
    cardVariant?: 'outlined' | 'elevation';
    labelIcon?: React.ReactNode;
    items: StatusTablePropsItem[];
    dense?: boolean;
}
