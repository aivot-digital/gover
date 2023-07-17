import {ApiDetailsResponse} from './api-details-response';

export interface ApiListResponse<T, A extends string> {
    _embedded: {
        [key in A]: ApiDetailsResponse<T>[];
    };
    _links: {
        [key: string]: {
            href: string;
        };
        self: {
            href: string;
        };
    };
    page: {
        size: number;
        totalElements: number;
        totalPages: number;
        number: number;
    };
}
