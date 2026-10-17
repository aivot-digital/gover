import {type Api} from '../../hooks/use-api';
import {type Page} from '../../models/dtos/page';
import {type CustomLink, type CustomLinkRequest, CustomLinkType} from './models/custom-link';

export class CustomLinksApiService {
    public constructor(private readonly api: Api) {
    }

    public list(type: CustomLinkType): Promise<Page<CustomLink>> {
        return this.api.get<Page<CustomLink>>('custom-links/', {
            queryParams: {page: 0, size: 999, sort: 'position,ASC', type},
        });
    }

    public listAvailable(type: CustomLinkType): Promise<Page<CustomLink>> {
        return this.api.get<Page<CustomLink>>('custom-links/available/', {
            queryParams: {page: 0, size: 999, sort: 'position,ASC', type},
        });
    }

    public create(request: CustomLinkRequest): Promise<CustomLink> {
        return this.api.post<CustomLink>('custom-links/', request);
    }

    public update(id: number, request: CustomLinkRequest): Promise<CustomLink> {
        return this.api.put<CustomLink>(`custom-links/${id}/`, request);
    }

    public destroy(id: number): Promise<void> {
        return this.api.destroy(`custom-links/${id}/`);
    }

    public reorder(type: CustomLinkType, ids: number[]): Promise<CustomLink[]> {
        return this.api.put<CustomLink[]>('custom-links/order/', {type, ids});
    }
}
