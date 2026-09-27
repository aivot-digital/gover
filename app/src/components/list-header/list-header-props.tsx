import {BadgeProps} from '@mui/material';

type ListHeaderAction = {icon: React.ReactNode, badge?: BadgeProps} & ({label: string} | {tooltip: string}) & ({onClick: (event: React.MouseEvent<HTMLButtonElement>) => void} | {link: string} | {href: string, target?: string;});

export interface ListHeaderProps {
    title: string;
    search: string | undefined;
    searchLabel: string;
    searchPlaceholder?: string;
    onSearchChange: (search: string) => void;
    actions?: ListHeaderAction[];
    hint?: {
        text: string;
        moreLink?: string;
    };
    smallTitle?: boolean;
}
