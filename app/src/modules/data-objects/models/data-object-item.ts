export interface DataObjectItem {
    id: string;
    schemaKey: string;
    data: Record<string, unknown>;
    created: string;
    updated: string;
}
