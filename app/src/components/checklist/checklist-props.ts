export interface ChecklistItem {
    label: string;
    done: boolean;
}

export interface ChecklistProps {
    items: ChecklistItem[];
}
