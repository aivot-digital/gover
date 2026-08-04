import {type StorageProviderMetadataAttribute} from '../../storage/entities/storage-provider-entity';

export interface AssetStorageProvider {
    id: number;
    name: string;
    readOnlyStorage: boolean;
    maxFileSizeInBytes: number;
    metadataAttributes: StorageProviderMetadataAttribute[];
}
