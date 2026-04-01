import {Asset} from './models/asset';
import {createApiPath} from '../../utils/url-path-utils';
import {BaseApiService} from '../../services/base-api-service';
import {Api} from '../../hooks/use-api';

export class AssetsApiService extends BaseApiService {
    constructor(api?: Api) {
        super();
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
            await this.postFormData<any>(
                this.buildFileApiPath(storageProviderId, storagePathFromRoot),
                formData,
            ),
        );
    }

    public async retrieveInStorageProvider(storagePathFromRoot: string, storageProviderId: number): Promise<Asset> {
        return AssetsApiService.mapViewItemToAsset(
            await this.get<any>(this.buildFileApiPath(storageProviderId, storagePathFromRoot)),
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
            await this.putFormData<any>(
                this.buildFileApiPath(storageProviderId, storagePathFromRoot),
                formData,
            ),
        );
    }

    public async updateMetadataInStorageProvider(storagePathFromRoot: string, metadata: Record<string, unknown>, storageProviderId: number): Promise<Asset> {
        return AssetsApiService.mapViewItemToAsset(
            await this.put<Record<string, unknown>, any>(
                this.buildFileMetadataApiPath(storageProviderId, storagePathFromRoot),
                metadata ?? {},
            ),
        );
    }

    public async destroyInStorageProvider(storagePathFromRoot: string, storageProviderId: number): Promise<void> {
        return await this.delete(this.buildFileApiPath(storageProviderId, storagePathFromRoot));
    }

    public async downloadContentInStorageProvider(storagePathFromRoot: string, storageProviderId: number, download: boolean = true): Promise<Blob> {
        const normalized = AssetsApiService.normalizeStoragePath(storagePathFromRoot);
        if (normalized === '/' || normalized.endsWith('/')) {
            throw new Error('Invalid file path');
        }

        const encodedPath = AssetsApiService.encodeStoragePathForRoute(normalized);
        return await this.getBlob(
            `/api/assets/${storageProviderId}/files-content/${encodedPath}`,
            {
                query: {
                    download,
                },
            },
        );
    }

    public async listFolderContent(storageProviderId: number, folderPath: string = '/'): Promise<Asset[]> {
        const items = await this.get<any[]>(this.buildFolderApiPath(storageProviderId, folderPath));
        return items.map(AssetsApiService.mapViewItemToAsset);
    }

    public async createFolder(storageProviderId: number, folderPath: string): Promise<void> {
        await this.post(this.buildFolderApiPath(storageProviderId, folderPath), {});
    }

    public async deleteFolder(storageProviderId: number, folderPath: string): Promise<void> {
        await this.delete(this.buildFolderApiPath(storageProviderId, folderPath));
    }

    public async moveInStorageProvider(storageProviderId: number, sourcePathFromRoot: string, targetPathFromRoot: string): Promise<Asset> {
        return AssetsApiService.mapViewItemToAsset(
            await this.post(
                `/api/assets/${storageProviderId}/move-file/`,
                {
                    sourcePath: AssetsApiService.normalizeStoragePath(sourcePathFromRoot),
                    targetPath: AssetsApiService.normalizeStoragePath(targetPathFromRoot),
                },
            ),
        );
    }

    public async copyInStorageProvider(storageProviderId: number, sourcePathFromRoot: string, targetPathFromRoot: string): Promise<Asset> {
        return AssetsApiService.mapViewItemToAsset(
            await this.post(
                `/api/assets/${storageProviderId}/copy-file/`,
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
        return `/api/public/assets/${asset.storageProviderId}/files${this.normalizeStoragePath(asset.storagePathFromRoot)}`;
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
            return `/api/assets/${storageProviderId}/folders/`;
        }

        const encodedPath = AssetsApiService.encodeStoragePathForRoute(normalized);
        return `/api/assets/${storageProviderId}/folders/${encodedPath}/`;
    }

    private buildFileApiPath(storageProviderId: number, filePathFromRoot: string) {
        const normalized = AssetsApiService.normalizeStoragePath(filePathFromRoot);
        if (normalized === '/' || normalized.endsWith('/')) {
            throw new Error('Invalid file path');
        }

        const encodedPath = AssetsApiService.encodeStoragePathForRoute(normalized);
        return `/api/assets/${storageProviderId}/files/${encodedPath}`;
    }

    private buildFileMetadataApiPath(storageProviderId: number, filePathFromRoot: string) {
        const normalized = AssetsApiService.normalizeStoragePath(filePathFromRoot);
        if (normalized === '/' || normalized.endsWith('/')) {
            throw new Error('Invalid file path');
        }

        const encodedPath = AssetsApiService.encodeStoragePathForRoute(normalized);
        return `/api/assets/${storageProviderId}/files-metadata/${encodedPath}`;
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
