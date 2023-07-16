export interface ListHeaderProps {
    title: string;
    search: string;
    searchPlaceholder: string;
    onSearchChange: (search: string) => void;
    actions: ({
        icon: JSX.Element;
        label: string;
        onClick: () => void;
    } | {
        icon: JSX.Element;
        tooltip: string;
        onClick: () => void;
    })[];
}
