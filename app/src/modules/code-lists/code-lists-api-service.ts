import {BaseCrudApiService} from '../../services/base-crud-api-service';
import {Page} from '../../models/dtos/page';
import {SortOrder} from '../../components/generic-list/generic-list-props';
import {CodeList} from './models/code-list';
import {CodeListItem} from './models/code-list-item';
import {CodeListSourceType} from './enums/code-list-source-type';
import {CodeListStatus} from './enums/code-list-status';

export interface CodeListFilter {
    name: string;
}

export class CodeListsApiService extends BaseCrudApiService<CodeList, CodeList, CodeList, CodeList, number, CodeListFilter> {
    private static readonly utilsPath = '/api/code-list-utils/';

    constructor() {
        super('/api/code-lists/');
    }

    public initialize(): CodeList {
        return {
            id: 0,
            sourceType: CodeListSourceType.Manual,
            sourceRef: '',
            name: '',
            description: '',
            columns: ['value', 'label'],
            valueColumnIndex: 0,
            labelColumnIndex: 1,
            status: CodeListStatus.Synced,
            statusMessage: null,
            lastSync: null,
            created: '',
            updated: '',
        };
    }

    public async triggerUpdate(id: number, keepOutdated: boolean = true): Promise<{ status: string }> {
        return await this.get<{ status: string }>(`${this.buildPath(id)}update/`, {
            query: {
                keepOutdated,
            },
        });
    }

    public async listItems(
        codeListId: number,
        page: number,
        limit: number,
        sort?: keyof CodeListItem,
        order?: SortOrder,
    ): Promise<Page<CodeListItem>> {
        return await this.get<Page<CodeListItem>>(`${this.buildPath(codeListId)}items/`, {
            query: {
                page,
                size: limit,
                sort: sort != null && order != null ? `${String(sort)},${order}` : undefined,
            },
        });
    }

    public async createItem(codeListId: number, columns: string[]): Promise<CodeListItem> {
        return await this.post<Partial<CodeListItem>, CodeListItem>(`${this.buildPath(codeListId)}items/`, {
            id: 0,
            codeListId,
            columns,
        });
    }

    public async updateItem(codeListId: number, itemId: number, columns: string[]): Promise<CodeListItem> {
        return await this.put<Partial<CodeListItem>, CodeListItem>(`${this.buildPath(codeListId)}items/${itemId}/`, {
            id: itemId,
            codeListId,
            columns,
        });
    }

    public async deleteItem(codeListId: number, itemId: number): Promise<void> {
        return await this.delete(`${this.buildPath(codeListId)}items/${itemId}/`);
    }

    public async getAssetColumns(assetKey: string): Promise<string[]> {
        return await this.get<string[]>(`${CodeListsApiService.utilsPath}asset/${encodeURIComponent(assetKey)}/columns/`);
    }

    public async getXRepositoryColumns(urn: string): Promise<string[]> {
        return await this.get<string[]>(`${CodeListsApiService.utilsPath}x-repository/${encodeURIComponent(urn)}/columns/`);
    }
}
