import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {Asset} from './models/asset';

interface AssetFilter {
    filename: string;
    uploaderId: string;
    contentType: string;
    isPrivate: boolean;
}

export class AssetsApiService extends CrudApiService<Asset, Asset, Asset, Asset, Asset, string, AssetFilter> {
    public constructor(api: Api) {
        super(api, 'assets/');
    }

    public async upload(file: File): Promise<Asset> {
        const formData = new FormData();
        formData.set('file', file);
        return await this.api.postFormData<Asset>('assets/', formData);
    }

    public static useAssetLink(key: string) {
        return `/api/public/assets/${key}`;
    }

    public static useAssetLinkOfAsset(asset: Asset) {
        return window.location.protocol + '//' + window.location.host + this.useAssetLink(asset.key);
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