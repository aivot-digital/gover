export type ApiDetailsResponse<T> = T & {
    _links: {
        [key: string]: {
            href: string;
        };
        self: {
            href: string;
        };
    };
}