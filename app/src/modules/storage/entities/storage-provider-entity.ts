import {type ElementData} from '../../../models/element-data';
import {type StorageProviderStatus} from '../enums/storage-provider-status';
import {type StorageProviderType} from '../enums/storage-provider-type';

export interface StorageProviderMetadataAttribute {
    label: string;
    key: string;
    description?: string;
}

export interface StorageProviderEntity {
    id: number;
    storageProviderDefinitionKey: string;
    storageProviderDefinitionVersion: number;
    name: string;
    description: string;
    type: StorageProviderType;
    status: StorageProviderStatus;
    statusMessage?: string | null;
    readOnly: boolean;
    configuration: ElementData;
    maxFileSizeInBytes: number;
    preventDeletion: boolean;
    metadataAttributes: StorageProviderMetadataAttribute[];
    created: string; // ISO date string
    updated: string; // ISO date string
    lastSync: string; // ISO date string
}
