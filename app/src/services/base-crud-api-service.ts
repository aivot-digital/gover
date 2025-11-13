import {Page} from '../models/dtos/page';
import {SortOrder} from '../components/generic-list/generic-list-props';
import {BaseApiService} from './base-api-service';

export abstract class BaseCrudApiService<CreateRequest, ListRes, DetailsRes, UpdateRequest, Id, Filter> extends BaseApiService {
    protected readonly path: string;

    protected constructor(path: string) {
        super();
        this.path = path;
    }

    public buildPath(id: Id): string {
        return `${this.path}${id}/`;
    }

    public async create(entity: CreateRequest): Promise<DetailsRes> {
        return await this.post<CreateRequest, DetailsRes>(this.path, entity, {});
    }

    public async listAll(filters?: Partial<Filter>) {
        return await this.list(0, 999, undefined, undefined, filters);
    }

    public async listAllOrdered(
        sort?: keyof DetailsRes extends string ? keyof DetailsRes : never,
        order?: SortOrder,
        filters?: Partial<Filter>,
    ) {
        return await this.list(0, 999, sort, order, filters);
    }

    public async list(
        page: number,
        limit: number,
        sort?: keyof DetailsRes extends string ? keyof DetailsRes : never,
        order?: SortOrder,
        filters?: Partial<Filter>,
    ): Promise<Page<ListRes>> {
        return await this.get<Page<ListRes>>(this.path, {
            query: {
                page: page,
                size: limit,
                sort: sort != null && order != null ? `${sort},${order}` : undefined,
                ...filters,
            },
        });
    }


    public abstract initialize(): DetailsRes;

    public async retrieve(id: Id): Promise<DetailsRes> {
        return await this.get<DetailsRes>(this.buildPath(id), {});
    }

    public async update(id: Id, link: UpdateRequest): Promise<DetailsRes> {
        return await this.put<UpdateRequest, DetailsRes>(this.buildPath(id), link, {});
    }

    public async destroy(id: Id): Promise<void> {
        return await this.delete(this.buildPath(id), {});
    }
}
