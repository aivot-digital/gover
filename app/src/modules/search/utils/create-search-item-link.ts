import {AssetsApiService} from '../../assets/assets-api-service';
import {SearchItemResponseDto} from '../dtos/search-item-response-dto';

export function createSearchItemLink(searchItem: SearchItemResponseDto): string {
    const id = searchItem.id;
    const originTable = searchItem.originTable as string;
    const separatorIndex = id.indexOf(',');
    const idHead = separatorIndex >= 0 ? id.slice(0, separatorIndex) : id;
    const idTail = separatorIndex >= 0 ? id.slice(separatorIndex + 1) : undefined;

    switch (originTable) {
        case 'assets':
            if (idTail == null || idTail.length === 0) {
                return '/assets';
            }

            const storagePath = AssetsApiService.normalizeStoragePath(idTail);
            const encodedStoragePath = AssetsApiService.encodeStoragePathForRoute(storagePath);
            const lastSlashIndex = storagePath.lastIndexOf('/');
            const parentFolder = lastSlashIndex <= 0
                ? '/'
                : AssetsApiService.normalizeFolderPath(storagePath.slice(0, lastSlashIndex));

            return `/assets/providers/${idHead}/files/${encodedStoragePath}?path=${encodeURIComponent(parentFolder)}`;
        case 'departments':
            return `/departments/${id}`;
        case 'data_object_items':
            return idTail != null ? `/data-objects/${idHead}/${idTail}` : '/data-objects';
        case 'data_object_schemas':
            return `/data-models/${id}`;
        case 'destinations':
            return '/destinations/' + id;
        case 'forms':
            return idTail != null ? `/forms/${idHead}/${idTail}` : '/forms';
        case 'identity_providers':
            return '/identity-providers/' + id;
        case 'payment_providers':
            return '/payment-providers/' + id;
        case 'storage_providers':
            return '/storage-providers/' + id;
        case 'presets':
            return idTail != null ? `/presets/edit/${idHead}/${idTail}` : '/presets';
        case 'provider_links':
            return '/provider-links/' + id;
        case 'secrets':
            return '/secrets/' + id;
        case 'teams':
            return '/teams/' + id;
        case 'submissions':
            return '/submissions/' + id;
        case 'themes':
            return '/themes/' + id;
        case 'domain_roles':
            return '/user-roles/' + id;
        case 'system_roles':
            return '/system-roles/' + id;
        case 'processes':
            return idTail != null ? `/processes/${idHead}/versions/${idTail}` : '/processes';
        case 'process_instances':
            return `/process-instances?search=${encodeURIComponent(id)}`;
        default:
            return '/not-found';
    }
}
