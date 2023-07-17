import {SvgIconProps} from "@mui/material";

export type AppToolbarPropsAction = ({
    tooltip: string;
    icon: SvgIconProps;
    onClick: () => void;
} | {
    tooltip: string;
    icon: SvgIconProps;
    href: string;
} | 'separator');

export interface AppToolbarProps {
    title: string;
    actions?: AppToolbarPropsAction[];
    noPlaceholder?: boolean;
}
