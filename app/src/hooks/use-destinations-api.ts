import {Destination} from '../models/entities/destination';
import {Api} from './use-api';

interface DestinationsApi {
    list(filter?: {ids?: number[]}): Promise<Destination[]>;

    retrieve(id: number): Promise<Destination>;

    save(link: Destination): Promise<Destination>;

    destroy(id: number): Promise<void>;
}

export function useDestinationsApi(api: Api): DestinationsApi {

    const list = async (filter?: {ids?: number[]}) => {
        return await api.get<Destination[]>('destinations', filter != null ? {
            id: filter.ids,
        } : undefined);
    };

    const retrieve = async (id: number) => {
        return await api.get<Destination>(`destinations/${id}`);
    };

    const save = async (link: Destination) => {
        if (link.id <= 0) {
            return await api.post<Destination>('destinations', link);
        } else {
            return await api.put<Destination>(`destinations/${link.id}`, link);
        }
    };

    const destroy = async (id: number) => {
        return await api.destroy<void>(`destinations/${id}`);
    };

    return {
        list,
        retrieve,
        save,
        destroy,
    };
}
