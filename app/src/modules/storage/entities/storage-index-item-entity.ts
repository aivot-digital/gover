import {type StorageProviderType} from '../enums/storage-provider-type';

export interface StorageIndexItem {
    storageProviderId: number;
    storageProviderType: StorageProviderType;
    pathFromRoot: string;
    isDirectory: boolean;
    filename: string;
    mimeType: string;
}
