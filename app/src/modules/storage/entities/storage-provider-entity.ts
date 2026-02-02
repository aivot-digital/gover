import {type ElementData} from '../../../models/element-data';
import {StorageProviderStatus} from '../enums/storage-provider-status';

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

export enum StorageProviderType {
    Assets = 'Assets',
    Attachments = 'Attachments',
}

