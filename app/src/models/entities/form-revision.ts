export interface FormRevision {
    id: string;
    formId: number;
    userId: string;
    timestamp: string;
    diff: DiffItem[];
}

export interface DiffItem {
    field: string;
    oldValue: any;
    newValue: any;
}