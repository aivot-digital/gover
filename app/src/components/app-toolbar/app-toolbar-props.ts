import {type SvgIconProps} from '@mui/material';

export type AppToolbarPropsAction = ({
    tooltip: string;
    icon: SvgIconProps;
    onClick: () => void;
} | {
    tooltip: string;
    icon: SvgIconProps;
    href: string;
} | {
    tooltip: string;
    icon: SvgIconProps;
    label: string;
    onClick: () => void;
} | {
    tooltip: string;
    icon: SvgIconProps;
    label: string;
    href: string;
} | 'separator');

export interface AppToolbarProps {
    title: string;
    actions?: AppToolbarPropsAction[];
    updateToolbarHeight?: (height: number) => void;
}
