import {ApiConfig} from '../api-config';
import {SystemAssetKeys} from '../data/system-asset-keys';

export class SystemAssetsService {
    public static getLogoLink() {
        return ApiConfig.address + '/public/system-assets/' + SystemAssetKeys.provider.logo;
    }

    public static getFaviconLink() {
        return ApiConfig.address + '/public/system-assets/' + SystemAssetKeys.provider.favicon;
    }
}