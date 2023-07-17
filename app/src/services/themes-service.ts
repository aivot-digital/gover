import {ApiService} from './api-service';
import {type Theme} from '../models/entities/theme';
import {type ListApplication} from '../models/entities/list-application';

class _ThemesService extends ApiService<Theme, Theme, number> {
    constructor() {
        super('themes');
    }

    public async retrievePublic(id: number): Promise<Theme> {
        return await ApiService.get(`public/themes/${id}`);
    }

    public async listApplications(destinationId: number): Promise<ListApplication[]> {
        return await ApiService.get<ListApplication[]>(`themes/${destinationId}/applications`);
    }
}

export const ThemesService = new _ThemesService();
