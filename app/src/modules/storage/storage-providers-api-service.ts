import type {StorageProviderDefinition} from './entities/storage-provider-definition';
import {BaseCrudApiService} from '../../services/base-crud-api-service';
import {
    type StorageProviderEntity,
} from './entities/storage-provider-entity';
import {StorageProviderStatus} from './enums/storage-provider-status';
import {StorageProviderType} from './enums/storage-provider-type';
import {type StorageIndexItem} from './entities/storage-index-item-entity';

export interface StorageProviderFilter {
    name: string;
    type: StorageProviderType;
    readOnlyStorage: boolean;
    systemProvider: boolean;
    storageProviderDefinitionKey: string;
}

export class StorageProvidersApiService extends BaseCrudApiService<StorageProviderEntity, StorageProviderEntity, StorageProviderEntity, StorageProviderEntity, number, StorageProviderFilter> {
    constructor() {
        super('/api/storage-providers/');
    }

    public async listDefinitions(): Promise<StorageProviderDefinition[]> {
        return await this.get<StorageProviderDefinition[]>('/api/storage-provider-definitions/', {});
    }

    public initialize(): StorageProviderEntity {
        return {
            configuration: {},
            created: '',
            description: '',
            id: 0,
            name: '',
            status: StorageProviderStatus.SyncPending,
            storageProviderDefinitionKey: '',
            storageProviderDefinitionVersion: 0,
            type: StorageProviderType.Assets,
            updated: '',
            lastSync: null,
            metadataAttributes: [],
            systemProvider: false,
            testProvider: false,
            readOnlyStorage: false,
            maxFileSizeInBytes: 0,
        };
    }

    public async resync(id: number): Promise<StorageProviderEntity> {
        return await this.put<any, StorageProviderEntity>(`${this.buildPath(id)}resync/`, {});
    }

    public async getFolder(id: number, path: string): Promise<StorageIndexItem[]> {
        const items = await this.get<any[]>(`${this.buildPath(id)}folders${path}`, {});

        return items.map((item) => {
            const isDirectory = item.directory ?? item.isDirectory ?? String(item.pathFromRoot ?? '').endsWith('/');

            return {
                storageProviderId: item.storageProviderId,
                storageProviderType: item.storageProviderType,
                pathFromRoot: item.pathFromRoot,
                directory: isDirectory,
                isDirectory: isDirectory,
                filename: item.filename,
                mimeType: item.mimeType ?? '',
                sizeInBytes: Number(item.sizeInBytes ?? 0),
                missing: item.missing === true,
                metadata: (typeof item.metadata === 'object' && item.metadata != null) ? item.metadata : {},
                created: item.created ?? '',
                updated: item.updated ?? '',
            } as StorageIndexItem;
        });
    }

    public async testStorageProvider(id: number, writable: boolean = false): Promise<{ success: boolean; error?: string }> {
        return await this.post<any, { success: boolean; error?: string }>(`${this.buildPath(id)}test/?writable=${writable}`, {});
    }
}
