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

    public async upload(file: File, storageProviderId: number, storagePathFromRoot: string, existingAsset?: Partial<Asset>): Promise<Asset> {
        const formData = new FormData();
        formData.set('file', file);

        const normalizedPath = AssetsApiService.normalizeStoragePath(storagePathFromRoot);
        const data = {
            storagePathFromRoot: normalizedPath,
            isPrivate: existingAsset?.isPrivate ?? true,
            metadata: existingAsset?.metadata ?? {},
        };
        formData.set('data', new Blob([JSON.stringify(data)], {type: 'application/json'}));

        const encodedPath = AssetsApiService.encodeStoragePathForRoute(normalizedPath);
        return await this.api.postFormData<Asset>(`assets/path/${encodedPath}?storageProviderId=${storageProviderId}`, formData);
    }

    public async retrieveInStorageProvider(storagePathFromRoot: string, storageProviderId: number): Promise<Asset> {
        const normalizedPath = AssetsApiService.normalizeStoragePath(storagePathFromRoot);
        const encodedPath = AssetsApiService.encodeStoragePathForRoute(normalizedPath);
        return await this.api.get<Asset>(`assets/path/${encodedPath}`, {
            queryParams: {
                storageProviderId,
            },
        });
    }

    public async updateInStorageProvider(storagePathFromRoot: string, asset: Asset, storageProviderId: number): Promise<Asset> {
        const normalizedPath = AssetsApiService.normalizeStoragePath(storagePathFromRoot);
        const encodedPath = AssetsApiService.encodeStoragePathForRoute(normalizedPath);

        return await this.api.put<Asset>(`assets/path/${encodedPath}?storageProviderId=${storageProviderId}`, {
            storagePathFromRoot: normalizedPath,
            filename: asset.filename,
            isPrivate: asset.isPrivate,
            contentType: asset.contentType ?? 'application/octet-stream',
            metadata: asset.metadata ?? {},
        });
    }

    public async destroyInStorageProvider(storagePathFromRoot: string, storageProviderId: number): Promise<void> {
        const normalizedPath = AssetsApiService.normalizeStoragePath(storagePathFromRoot);
        const encodedPath = AssetsApiService.encodeStoragePathForRoute(normalizedPath);
        return await this.api.destroy<void>(`assets/path/${encodedPath}?storageProviderId=${storageProviderId}`);
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

    public static normalizeStoragePath(storagePathFromRoot: string) {
        const trimmed = storagePathFromRoot.trim();
        if (trimmed.length === 0) {
            return '/';
        }
        return trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
    }

    public static encodeStoragePathForRoute(storagePathFromRoot: string) {
        const normalized = AssetsApiService.normalizeStoragePath(storagePathFromRoot);
        const segments = normalized
            .split('/')
            .filter((segment) => segment.length > 0)
            .map((segment) => encodeURIComponent(segment));
        return segments.join('/');
    }

    public static decodeStoragePathFromRoute(storagePathFromRoute: string) {
        const normalized = storagePathFromRoute.trim().replace(/^\/+/, '');
        if (normalized.length === 0) {
            return '/';
        }

        const segments = normalized
            .split('/')
            .filter((segment) => segment.length > 0)
            .map((segment) => decodeURIComponent(segment));

        return `/${segments.join('/')}`;
    }

    public initialize(): Asset {
        return {
            key: '',
            storageProviderId: 0,
            storagePathFromRoot: '',
            contentType: '',
            filename: '',
            created: new Date().toISOString(),
            uploaderId: '',
            isPrivate: true,
            metadata: {},
        };
    }
}
