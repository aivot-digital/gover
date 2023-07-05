import {IconDefinition} from '@fortawesome/pro-duotone-svg-icons';


export type AppToolbarPropsAction = ({
    tooltip: string;
    icon: IconDefinition;
    onClick: () => void;
} | {
    tooltip: string;
    icon: IconDefinition;
    href: string;
} | 'separator');

export interface AppToolbarProps {
    title: string;
    actions?: AppToolbarPropsAction[];
    noPlaceholder?: boolean;
}
