import {BaseCrudApiService} from '../../services/base-crud-api-service';
import {Page} from '../../models/dtos/page';
import {SortOrder} from '../../components/generic-list/generic-list-props';
import {CodeList, CodeListSourceColumns} from './models/code-list';
import {CodeListItem} from './models/code-list-item';
import {CodeListSourceType} from './enums/code-list-source-type';
import {CodeListStatus} from './enums/code-list-status';
import {type SelectFieldElementOption} from '../../models/elements/form/input/select-field-element';

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
            columnMappings: [
                {key: 'Beschriftung', name: 'Beschriftung', index: 1},
                {key: 'Wert', name: 'Wert', index: 2},
            ],
            keyColumnKey: 'Wert',
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

    /** Lists the options an input element receives for the given columns of the code list. */
    public async listOptions(
        codeListKey: string,
        valueColumnKey: string,
        labelColumnKey: string,
        groupColumnKey?: string | null,
    ): Promise<SelectFieldElementOption[]> {
        return await this.get<SelectFieldElementOption[]>(`${this.buildPath(codeListKey)}options/`, {
            query: {
                valueColumnKey,
                labelColumnKey,
                groupColumnKey: groupColumnKey ?? undefined,
            },
        });
    }

    public async getAssetColumns(assetKey: string): Promise<CodeListSourceColumns> {
        return await this.get<CodeListSourceColumns>(`${CodeListsApiService.utilsPath}asset/${encodeURIComponent(assetKey)}/columns/`);
    }

    public async getXRepositoryColumns(urn: string): Promise<CodeListSourceColumns> {
        return await this.get<CodeListSourceColumns>(`${CodeListsApiService.utilsPath}x-repository/${encodeURIComponent(urn)}/columns/`);
    }
}
