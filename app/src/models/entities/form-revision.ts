export interface FormRevision {
    id: number;
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