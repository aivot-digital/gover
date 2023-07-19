type ListHeaderAction = {icon: JSX.Element} & ({label: string} | {tooltip: string}) & ({onClick: () => void} | {link: string});

export interface ListHeaderProps {
    title: string;
    search: string;
    searchPlaceholder: string;
    onSearchChange: (search: string) => void;
    actions?: ListHeaderAction[];
    hint?: {
        text: string;
        moreLink?: string;
    };
    smallTitle?: boolean;
}
