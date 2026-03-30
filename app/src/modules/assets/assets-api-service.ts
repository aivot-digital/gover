import {Api} from '../../hooks/use-api';
import {Asset} from './models/asset';
import {createApiPath} from '../../utils/url-path-utils';

export class AssetsApiService {
    private readonly api: Api;

    public constructor(api: Api) {
        this.api = api;
    }

    public async upload(file: File, storageProviderId: number, storagePathFromRoot: string, existingAsset?: Partial<Asset>): Promise<Asset> {
        const formData = new FormData();
        formData.set('file', file);

        const data = {
            isPrivate: existingAsset?.isPrivate ?? true,
            metadata: existingAsset?.metadata ?? {},
        };
        formData.set('data', new Blob([JSON.stringify(data)], {type: 'application/json'}));

        return AssetsApiService.mapViewItemToAsset(
            await this.api.postFormData<any>(
                this.buildFileApiPath(storageProviderId, storagePathFromRoot),
                formData,
            ),
        );
    }

    public async retrieveInStorageProvider(storagePathFromRoot: string, storageProviderId: number): Promise<Asset> {
        return AssetsApiService.mapViewItemToAsset(
            await this.api.get<any>(this.buildFileApiPath(storageProviderId, storagePathFromRoot)),
        );
    }

    public async updateInStorageProvider(storagePathFromRoot: string, asset: Asset, storageProviderId: number, file?: File): Promise<Asset> {
        const formData = new FormData();
        if (file != null) {
            formData.set('file', file);
        }
        formData.set(
            'data',
            new Blob(
                [
                    JSON.stringify({
                        isPrivate: asset.isPrivate,
                        metadata: file != null ? (asset.metadata ?? {}) : null,
                    }),
                ],
                {type: 'application/json'},
            ),
        );

        return AssetsApiService.mapViewItemToAsset(
            await this.api.putFormData<any>(
                this.buildFileApiPath(storageProviderId, storagePathFromRoot),
                formData,
            ),
        );
    }

    public async destroyInStorageProvider(storagePathFromRoot: string, storageProviderId: number): Promise<void> {
        return await this.api.destroy<void>(this.buildFileApiPath(storageProviderId, storagePathFromRoot));
    }

    public async downloadContentInStorageProvider(storagePathFromRoot: string, storageProviderId: number, download: boolean = true): Promise<Blob> {
        const normalized = AssetsApiService.normalizeStoragePath(storagePathFromRoot);
        if (normalized === '/' || normalized.endsWith('/')) {
            throw new Error('Invalid file path');
        }

        const encodedPath = AssetsApiService.encodeStoragePathForRoute(normalized);
        return await this.api.getBlob(
            `assets/${storageProviderId}/files-content/${encodedPath}`,
            {
                queryParams: {
                    download,
                },
            },
        );
    }

    public async listFolderContent(storageProviderId: number, folderPath: string = '/'): Promise<Asset[]> {
        const items = await this.api.get<any[]>(this.buildFolderApiPath(storageProviderId, folderPath));
        return items.map(AssetsApiService.mapViewItemToAsset);
    }

    public async createFolder(storageProviderId: number, folderPath: string): Promise<void> {
        await this.api.post<void>(this.buildFolderApiPath(storageProviderId, folderPath), {});
    }

    public async deleteFolder(storageProviderId: number, folderPath: string): Promise<void> {
        await this.api.destroy<void>(this.buildFolderApiPath(storageProviderId, folderPath));
    }

    public async moveInStorageProvider(storageProviderId: number, sourcePathFromRoot: string, targetPathFromRoot: string): Promise<Asset> {
        return AssetsApiService.mapViewItemToAsset(
            await this.api.post<any>(
                `assets/${storageProviderId}/move-file/`,
                {
                    sourcePath: AssetsApiService.normalizeStoragePath(sourcePathFromRoot),
                    targetPath: AssetsApiService.normalizeStoragePath(targetPathFromRoot),
                },
            ),
        );
    }

    public async copyInStorageProvider(storageProviderId: number, sourcePathFromRoot: string, targetPathFromRoot: string): Promise<Asset> {
        return AssetsApiService.mapViewItemToAsset(
            await this.api.post<any>(
                `assets/${storageProviderId}/copy-file/`,
                {
                    sourcePath: AssetsApiService.normalizeStoragePath(sourcePathFromRoot),
                    targetPath: AssetsApiService.normalizeStoragePath(targetPathFromRoot),
                },
            ),
        );
    }

    public static useAssetLink(key: string) {
        return createApiPath(`/api/public/assets/${key}/`);
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

    public static normalizeFolderPath(folderPathFromRoot: string) {
        let normalized = AssetsApiService.normalizeStoragePath(folderPathFromRoot);
        if (!normalized.endsWith('/')) {
            normalized = `${normalized}/`;
        }
        return normalized;
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

    private buildFolderApiPath(storageProviderId: number, folderPathFromRoot: string) {
        const normalized = AssetsApiService.normalizeFolderPath(folderPathFromRoot);
        if (normalized === '/') {
            return `assets/${storageProviderId}/folders/`;
        }

        const encodedPath = AssetsApiService.encodeStoragePathForRoute(normalized);
        return `assets/${storageProviderId}/folders/${encodedPath}/`;
    }

    private buildFileApiPath(storageProviderId: number, filePathFromRoot: string) {
        const normalized = AssetsApiService.normalizeStoragePath(filePathFromRoot);
        if (normalized === '/' || normalized.endsWith('/')) {
            throw new Error('Invalid file path');
        }

        const encodedPath = AssetsApiService.encodeStoragePathForRoute(normalized);
        return `assets/${storageProviderId}/files/${encodedPath}`;
    }

    private static mapViewItemToAsset(raw: any): Asset {
        return {
            key: raw.assetKey ?? '',
            storageProviderId: raw.storageProviderId,
            storagePathFromRoot: raw.pathFromRoot,
            filename: raw.filename,
            created: raw.created,
            uploaderId: raw.assetUploaderId ?? '',
            contentType: raw.mimeType ?? 'application/octet-stream',
            isPrivate: raw.assetIsPrivate ?? true,
            metadata: raw.metadata ?? {},
            directory: raw.directory ?? false,
            missing: raw.missing ?? false,
            sizeInBytes: raw.sizeInBytes ?? 0,
        };
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
            directory: false,
            missing: false,
            sizeInBytes: 0,
        };
    }
}
