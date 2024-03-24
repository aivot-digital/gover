import {Api} from './use-api';
import {Asset} from '../models/entities/asset';

interface AssetsApi {
    list(mimetype?: string): Promise<Asset[]>;

    retrieve(key: string): Promise<Asset>;

    create(file: File): Promise<Asset>;

    update(key: string, asset: Asset): Promise<Asset>;

    destroy(key: string): Promise<void>;
}

export function useAssetsApi(api: Api): AssetsApi {

    const list = async (mimetype?: string) => {
        return await api.get<Asset[]>('assets', {
            mimetype: mimetype,
        });
    };

    const retrieve = async (key: string) => {
        return await api.get<Asset>(`assets/${key}`);
    };

    const create = async (file: File) => {
        const formData = new FormData();
        formData.set('file', file);
        return await api.postFormData<Asset>('assets', formData);
    };

    const update = async (key: string, asset: Asset) => {
        return await api.put<Asset>(`assets/${key}`, asset);
    };

    const destroy = async (key: string) => {
        return await api.destroy<void>(`assets/${key}`);
    };

    return {
        list,
        retrieve,
        create,
        update,
        destroy,
    };
}

export function useAssetLink(key: string) {
    return `/api/public/assets/${key}`;
}

export function useAssetLinkOfAsset(asset: Asset) {
    return window.location.protocol + '//' + window.location.host + useAssetLink(asset.key);
}