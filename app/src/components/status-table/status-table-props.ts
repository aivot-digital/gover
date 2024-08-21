import {PropsWithChildren} from 'react';
import {SxProps, TypographyVariant} from '@mui/material';

export type StatusTablePropsItem = PropsWithChildren<{
    label: string;
    icon: JSX.Element;
    alignTop?: boolean;
    subItems?: StatusTablePropsItem[] | null;
}>;

export interface StatusTableProps {
    sx?: SxProps;
    label: string;
    labelVariant?: TypographyVariant;
    labelSx?: SxProps;
    description?: string;
    descriptionSx?: SxProps;
    cardSx?: SxProps;
    cardVariant?: 'outlined' | 'elevation';
    labelIcon?: JSX.Element;
    items: StatusTablePropsItem[];
}