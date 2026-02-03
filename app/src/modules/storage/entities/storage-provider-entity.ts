import {type ElementData} from '../../../models/element-data';
import {type StorageProviderStatus} from '../enums/storage-provider-status';
import {type StorageProviderType} from '../enums/storage-provider-type';

export interface StorageProviderEntity {
    id: number;
    storageProviderDefinitionKey: string;
    storageProviderDefinitionVersion: number;
    name: string;
    description: string;
    type: StorageProviderType;
    status: StorageProviderStatus;
    statusMessage?: string | null;
    configuration: ElementData;
    created: string; // ISO date string
    updated: string; // ISO date string
}
