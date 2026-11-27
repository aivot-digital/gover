import {SortOrder} from '../components/generic-list/generic-list-props';
import {Page} from '../models/dtos/page';
import {BaseApiService} from './base-api-service';

export abstract class BaseReadApiService<ListRes, DetailsRes, Id, Filter, SortFields = keyof DetailsRes extends string ? keyof DetailsRes : never> extends BaseApiService {
    protected readonly path: string;

    protected constructor(path: string) {
        super();
        this.path = path;
    }

    public buildPath(id: Id): string {
        return `${this.path}${id}/`;
    }

    public async listAll(filters?: Partial<Filter>) {
        return await this.list(0, 999, undefined, undefined, filters);
    }

    public async listAllOrdered(
        sort?: SortFields | SortFields[],
        order?: SortOrder,
        filters?: Partial<Filter>,
    ) {
        return await this.list(0, 999, sort, order, filters);
    }

    public async list(
        page: number,
        limit: number,
        sort?: SortFields | SortFields[],
        order?: SortOrder,
        filters?: Partial<Filter>,
    ): Promise<Page<ListRes>> {
        return await this.get<Page<ListRes>>(this.path, {
            query: {
                page,
                size: limit,
                sort: sort != null && order != null ? (Array.isArray(sort) ? sort.map(s => `${s},${order}`) : `${sort},${order}`) : undefined,
                ...filters,
            },
        });
    }

    public async retrieve(id: Id): Promise<DetailsRes> {
        return await this.get<DetailsRes>(this.buildPath(id), {});
    }

    public abstract initialize(): DetailsRes;
}
