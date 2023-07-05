export interface StoreListResponse<T> {
    page: number;
    size: number;
    total: number;
    items: T[];
}