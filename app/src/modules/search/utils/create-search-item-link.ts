import {SearchItemResponseDto} from '../dtos/search-item-response-dto';

export function createSearchItemLink(searchItem: SearchItemResponseDto): string {
    const id = searchItem.id;
    const idParts = searchItem.id.split(',');

    switch (searchItem.originTable) {
        case 'assets':
            return `/assets/${id}`;
        case 'departments':
            return `/departments/${id}`;
        case 'data_object_items':
            return `/data-objects/${idParts[0]}/items/${idParts[1]}`;
        case 'data_object_schemas':
            return `/data-objects/${id}`;
        case 'destinations':
            return '/destinations/' + id;
        case 'forms':
            return `/forms/${idParts[0]}/${idParts[1]}`;
        case 'identity_providers':
            return '/identity-providers/' + id;
        case 'payment_providers':
            return '/payment-providers/' + id;
        case 'presets':
            return `/presets/edit/${idParts[0]}/${idParts[1]}`;
        case 'provider_links':
            return '/provider-links/' + id;
        case 'secrets':
            return '/secrets/' + id;
        case 'submissions':
            return '/submissions/' + id;
        case 'themes':
            return '/themes/' + id;
        default:
            return '/not-found';
    }
}