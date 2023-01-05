import {IconDefinition} from '@fortawesome/pro-duotone-svg-icons';

export interface AppToolbarProps {
    title: string;
    parentPath: string;
    actions?: ({
        tooltip: string;
        icon: IconDefinition;
        onClick: () => void;
    } | {
        tooltip: string;
        icon: IconDefinition;
        href: string;
    } | 'separator')[];
    noPlaceholder?: boolean;
}
