import {type StorageProviderType} from '../enums/storage-provider-type';

export interface StorageIndexItem {
    storageProviderId: number;
    storageProviderType: StorageProviderType;
    pathFromRoot: string;
    directory: boolean;
    filename: string;
    mimeType: string;
    sizeInBytes: number;
    missing: boolean;
    metadata: Record<string, unknown>;
    created: string;
    updated: string;
    assetKey?: string;
    assetUploaderId?: string;
    assetIsPrivate?: boolean;
}

export interface VStorageIndexItemWithAssetEntity extends StorageIndexItem{
    assetKey: string;
    assetUploaderId: string;
    assetIsPrivate: boolean;
}
