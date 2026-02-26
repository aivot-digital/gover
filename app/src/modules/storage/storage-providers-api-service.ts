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
            lastSync: '',
            metadataAttributes: [],
            systemProvider: false,
            readOnlyStorage: false,
            maxFileSizeInBytes: 0,
        };
    }

    public async resync(id: number): Promise<StorageProviderEntity> {
        return await this.put<any, StorageProviderEntity>(`${this.buildPath(id)}resync/`, {});
    }

    public async getFolder(id: number, path: string): Promise<StorageIndexItem[]> {
        return await this.get<StorageIndexItem[]>(`${this.buildPath(id)}folders${path}`, {});
    }

    public async downloadFile(id: number, path: string): Promise<Blob> {
        return await this.getBlob(`${this.buildPath(id)}files${path}`, {});
    }

    public async testStorageProvider(id: number, writable: boolean = false): Promise<{ success: boolean; error?: string }> {
        return await this.post<any, { success: boolean; error?: string }>(`${this.buildPath(id)}test/?writable=${writable}`, {});
    }
}
