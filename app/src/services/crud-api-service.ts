import {Api} from '../hooks/use-api';
import {Page} from '../models/dtos/page';
import {SortOrder} from '../components/generic-list/generic-list-props';


export abstract class CrudApiService<Request, ListRes, PublicListRes, DetailsRes, PublicDetailsRes, Id, Filter> {
    protected readonly api: Api;
    protected readonly path: string;

    protected constructor(api: Api, path: string) {
        this.api = api;
        this.path = path.endsWith('/') ? path : `${path}/`;
    }

    public async create(entity: Request): Promise<DetailsRes> {
        return await this.api.post<DetailsRes>(this.path, entity, {});
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
        return await this.api.get<Page<ListRes>>(this.path, {
            queryParams: {
                page: page,
                size: limit,
                sort: sort != null && order != null ? `${sort},${order}` : undefined,
                ...filters,
            },
        });
    }

    public async listPublicAll(): Promise<Page<PublicListRes>> {
        return await this.listPublic(0, 999, undefined, undefined, {});
    }

    public async listPublic(
        page: number,
        limit: number,
        sort?: keyof DetailsRes extends string ? keyof DetailsRes : never,
        order?: SortOrder,
        filters?: Partial<Filter>,
    ): Promise<Page<PublicListRes>> {
        return await this.api.getPublic<Page<PublicListRes>>(this.path, {
            queryParams: {
                page: page,
                size: limit,
                sort: sort != null && order != null ? `${sort},${order}` : undefined,
                ...filters,
            },
        });
    }

    public abstract initialize(): DetailsRes;

    public async retrieve(id: Id): Promise<DetailsRes> {
        return await this.api.get<DetailsRes>(`${this.path}${id}/`, {});
    }

    public async retrievePublic(id: Id): Promise<PublicDetailsRes> {
        return await this.api.getPublic<PublicDetailsRes>(`${this.path}${id}/`, {});
    }


    public async update(id: Id, link: Request): Promise<DetailsRes> {
        return await this.api.put<DetailsRes>(`${this.path}${id}/`, link, {});
    }

    public async destroy(id: Id): Promise<void> {
        return await this.api.destroy<void>(`${this.path}${id}/`, {});
    }
}
