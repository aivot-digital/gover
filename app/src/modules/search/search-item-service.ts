import {BaseApiService, RequestOptions} from '../../services/base-api-service';
import {SearchItemResponseDto} from './dtos/search-item-response-dto';
import {Page} from '../../models/dtos/page';
import {ServerEntityType} from '../../shells/staff/data/server-entity-type';

export const DEFAULT_SEARCH_SIZE = 16;

export interface SearchRecentItemRequestDto {
    id: string;
    originTable: ServerEntityType;
}

export class SearchItemService extends BaseApiService {
    getSearchItems(search: string, page = 0, size = DEFAULT_SEARCH_SIZE, abort?: AbortSignal): Promise<Page<SearchItemResponseDto>> {
        return this.get<Page<SearchItemResponseDto>>('/api/search/', {
            query: new URLSearchParams({
                search: search,
                page: page.toString(),
                size: size.toString(),
            }),
            abort,
        });
    }

    getSearchItemsForTable(search: string, originTable: ServerEntityType, page = 0, size = DEFAULT_SEARCH_SIZE, abort?: AbortSignal): Promise<Page<SearchItemResponseDto>> {
        return this.get<Page<SearchItemResponseDto>>('/api/search/', {
            query: new URLSearchParams({
                search: search,
                originTable: originTable,
                page: page.toString(),
                size: size.toString(),
            }),
            abort,
        });
    }

    getRecentSearchItems(size = 10, abort?: AbortSignal): Promise<SearchItemResponseDto[]> {
        return this.get<SearchItemResponseDto[]>('/api/search/recent/', {
            query: new URLSearchParams({
                size: size.toString(),
            }),
            abort,
        });
    }

    recordRecentSearchItem(request: SearchRecentItemRequestDto, options?: RequestOptions): Promise<void> {
        return this.putWithoutResponse('/api/search/recent/', request, options);
    }
}
