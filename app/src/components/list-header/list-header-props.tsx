import {IconDefinition} from '@fortawesome/pro-duotone-svg-icons';

export interface ListHeaderProps {
    title: string;
    search: string;
    searchPlaceholder: string;
    onSearchChange: (search: string) => void;
    actions: ({
        icon: IconDefinition;
        label: string;
        onClick: () => void;
    } | {
        icon: IconDefinition;
        tooltip: string;
        onClick: () => void;
    })[];
}
