import {RequestOptions} from './base-api-service';
import {BaseReadApiService} from './base-read-api-service';

export abstract class BaseCrudApiService<CreateRequest, ListRes, DetailsRes, UpdateRequest, Id, Filter, SortFields = keyof DetailsRes extends string ? keyof DetailsRes : never> extends BaseReadApiService<ListRes, DetailsRes, Id, Filter, SortFields> {
    protected constructor(path: string) {
        super(path);
    }

    public async create(entity: CreateRequest): Promise<DetailsRes> {
        return await this.post<CreateRequest, DetailsRes>(this.path, entity, {});
    }

    public async update(id: Id, link: UpdateRequest, options?: RequestOptions): Promise<DetailsRes> {
        return await this.put<UpdateRequest, DetailsRes>(this.buildPath(id), link, options);
    }

    public async destroy(id: Id, options?: RequestOptions): Promise<void> {
        return await this.delete(this.buildPath(id), options);
    }
}
