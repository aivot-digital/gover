import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {Asset} from './models/asset';
import {AssetFolderGroup} from './models/asset-folder-group';
import {createApiPath} from '../../utils/url-path-utils';

interface AssetFilter {
    filename: string;
    uploaderId: string;
    contentType: string;
    isPrivate: boolean;
    storageProviderId: number;
}

export class AssetsApiService extends CrudApiService<Asset, Asset, Asset, Asset, Asset, string, AssetFilter> {
    public constructor(api: Api) {
        super(api, 'assets/');
    }

    public async upload(file: File, storageProviderId?: number): Promise<Asset> {
        const formData = new FormData();
        formData.set('file', file);
        const storageProviderQuery = storageProviderId != null ? `?storageProviderId=${storageProviderId}` : '';
        return await this.api.postFormData<Asset>(`assets/${storageProviderQuery}`, formData);
    }

    public async updateInStorageProvider(key: string, asset: Asset, storageProviderId?: number): Promise<Asset> {
        const storageProviderQuery = storageProviderId != null ? `?storageProviderId=${storageProviderId}` : '';
        return await this.api.put<Asset>(`assets/${key}/${storageProviderQuery}`, asset);
    }

    public async destroyInStorageProvider(key: string, storageProviderId?: number): Promise<void> {
        const storageProviderQuery = storageProviderId != null ? `?storageProviderId=${storageProviderId}` : '';
        return await this.api.destroy<void>(`assets/${key}/${storageProviderQuery}`);
    }

    public async listGroupedByFolder(path: string = '/'): Promise<AssetFolderGroup[]> {
        const normalizedPath = path.startsWith('/') ? path : `/${path}`;
        return await this.api.get<AssetFolderGroup[]>(`assets/folders${normalizedPath}`, {});
    }

    public static useAssetLink(key: string) {
        return createApiPath(`/api/public/assets/${key}`);
    }

    public static useAssetLinkOfAsset(asset: Asset) {
        return this.useAssetLink(asset.key);
    }

    public initialize(): Asset {
        return {
            key: '',
            contentType: '',
            filename: '',
            created: new Date().toISOString(),
            uploaderId: '',
            isPrivate: true,
        };
    }
}
