import {Api} from './use-api';
import {ProviderLink} from '../models/entities/provider-link';
import {Theme} from '../models/entities/theme';

interface ProviderLinksApi {
    listProviderLinks(): Promise<ProviderLink[]>;

    retrieveProviderLink(id: number): Promise<ProviderLink>;

    saveProviderLink(link: ProviderLink): Promise<ProviderLink>;

    destroyProviderLink(id: number): Promise<void>;
}

export function useProviderLinksApi(api: Api): ProviderLinksApi {

    const listProviderLinks = async () => {
        return await api.get<ProviderLink[]>('provider-links');
    };

    const retrieveProviderLink = async (id: number) => {
        return await api.get<ProviderLink>(`provider-links/${id}`);
    };

    const saveProviderLink = async (link: ProviderLink) => {
        if (link.id <= 0) {
            return await api.post<ProviderLink>('provider-links', link);
        } else {
            return await api.put<ProviderLink>(`provider-links/${link.id}`, link);
        }
    };

    const destroyProviderLink = async (id: number) => {
        return await api.destroy<void>(`provider-links/${id}`);
    };

    return {
        listProviderLinks,
        retrieveProviderLink,
        saveProviderLink,
        destroyProviderLink,
    };
}
