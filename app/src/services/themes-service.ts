import { ApiService } from './api-service';
import { type Theme } from '../models/entities/theme';

class _ThemesService extends ApiService<Theme, Theme, number> {
    constructor() {
        super('themes');
    }

    public async retrievePublic(id: number): Promise<Theme[]> {
        return await ApiService.get(`public/themes/${ id }`);
    }
}

export const ThemesService = new _ThemesService();
