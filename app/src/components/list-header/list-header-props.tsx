import { type IconDefinition } from '@fortawesome/pro-duotone-svg-icons';

type ListHeaderAction = { icon: IconDefinition } & ({ label: string } | { tooltip: string }) & ({ onClick: () => void } | { link: string });

export interface ListHeaderProps {
    title: string;
    search: string;
    searchPlaceholder: string;
    onSearchChange: (search: string) => void;
    actions: ListHeaderAction[];
}
