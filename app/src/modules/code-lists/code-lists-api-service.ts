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

export class CodeListsApiService extends BaseCrudApiService<CodeList, CodeList, CodeList, CodeList, string, CodeListFilter> {
    private static readonly utilsPath = '/api/code-list-utils/';

    constructor() {
        super('/api/code-lists/');
    }

    public buildPath(key: string): string {
        return `${this.path}${encodeURIComponent(key)}/`;
    }

    public initialize(): CodeList {
        return {
            key: '',
            id: 0,
            sourceType: CodeListSourceType.Manual,
            sourceRef: '',
            name: '',
            description: '',
            columns: ['Beschriftung', 'Wert'],
            valueColumnIndex: 1,
            labelColumnIndex: 0,
            status: CodeListStatus.Synced,
            statusMessage: null,
            lastSync: null,
            created: '',
            updated: '',
        };
    }

    public async triggerUpdate(codeListKey: string, keepOutdated: boolean = true): Promise<{ status: string }> {
        return await this.get<{ status: string }>(`${this.buildPath(codeListKey)}update/`, {
            query: {
                keepOutdated,
            },
        });
    }

    public async exportCsv(codeListKey: string): Promise<Blob> {
        return await this.getBlob(`${this.buildPath(codeListKey)}export.csv`);
    }

    public async importCsv(codeListKey: string, file: File): Promise<CodeList> {
        const formData = new FormData();
        formData.append('file', file);
        return await this.postFormData<CodeList>(`${this.buildPath(codeListKey)}import.csv`, formData);
    }

    public async listItems(
        codeListKey: string,
        page: number,
        limit: number,
        sort?: keyof CodeListItem,
        order?: SortOrder,
    ): Promise<Page<CodeListItem>> {
        return await this.get<Page<CodeListItem>>(`${this.buildPath(codeListKey)}items/`, {
            query: {
                page,
                size: limit,
                sort: sort != null && order != null ? `${String(sort)},${order}` : undefined,
            },
        });
    }

    public async createItem(codeListKey: string, columns: string[]): Promise<CodeListItem> {
        return await this.post<Partial<CodeListItem>, CodeListItem>(`${this.buildPath(codeListKey)}items/`, {
            id: 0,
            columns,
        });
    }

    public async updateItem(codeListKey: string, itemId: number, columns: string[]): Promise<CodeListItem> {
        return await this.put<Partial<CodeListItem>, CodeListItem>(`${this.buildPath(codeListKey)}items/${itemId}/`, {
            id: itemId,
            columns,
        });
    }

    public async deleteItem(codeListKey: string, itemId: number): Promise<void> {
        return await this.delete(`${this.buildPath(codeListKey)}items/${itemId}/`);
    }

    public async getAssetColumns(assetKey: string): Promise<string[]> {
        return await this.get<string[]>(`${CodeListsApiService.utilsPath}asset/${encodeURIComponent(assetKey)}/columns/`);
    }

    public async getXRepositoryColumns(urn: string): Promise<string[]> {
        return await this.get<string[]>(`${CodeListsApiService.utilsPath}x-repository/${encodeURIComponent(urn)}/columns/`);
    }
}
