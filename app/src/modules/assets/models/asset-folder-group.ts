import {Asset} from './asset';
import {StorageIndexItem} from '../../storage/entities/storage-index-item-entity';

export interface AssetFolderGroup {
    storageProviderId: number;
    storageProviderName: string;
    folders: StorageIndexItem[];
    files: Asset[];
}
