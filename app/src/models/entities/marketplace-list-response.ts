export interface MarketplaceListResponse<T> {
    page: number;
    size: number;
    total: number;
    items: T[];
}