import {BaseApiService} from '../../services/base-api-service';
import {SearchItemResponseDto} from './dtos/search-item-response-dto';
import {Page} from '../../models/dtos/page';
import {ServerEntityType} from '../../shells/staff/data/server-entity-type';

export const DEFAULT_SEARCH_SIZE = 16;

export class SearchItemService extends BaseApiService {
    getSearchItems(search: string, page = 0, size= DEFAULT_SEARCH_SIZE): Promise<Page<SearchItemResponseDto>> {
        return this.get<Page<SearchItemResponseDto>>('/api/search/', {
            query: new URLSearchParams({
                search: search,
                page: page.toString(),
                size: size.toString(),
            }),
        });
    }

    getSearchItemsForTable(search: string, originTable: ServerEntityType, page = 0, size= DEFAULT_SEARCH_SIZE): Promise<Page<SearchItemResponseDto>> {
        return this.get<Page<SearchItemResponseDto>>('/api/search/', {
            query: new URLSearchParams({
                search: search,
                originTable: originTable,
                page: page.toString(),
                size: size.toString(),
            }),
        });
    }
}