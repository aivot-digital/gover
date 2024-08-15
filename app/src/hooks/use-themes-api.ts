import {Api} from './use-api';
import {Theme} from '../models/entities/theme';
import {FormListProjection} from '../models/entities/form';

interface ThemesApi {
    listThemes(): Promise<Theme[]>;
    listPublicThemes(): Promise<Theme[]>;

    retrieveTheme(id: number): Promise<Theme>;
    retrievePublicTheme(id: number): Promise<Theme>;

    saveTheme(theme: Theme): Promise<Theme>;

    deleteTheme(id: number): Promise<void>;
}

export function useThemesApi(api: Api): ThemesApi {

    const listThemes = async () => {
        return await api.get<Theme[]>('themes');
    };

    const listPublicThemes = async () => {
        return await api.getPublic<Theme[]>('themes');
    };

    const retrieveTheme = async (id: number) => {
        return await api.get<Theme>(`themes/${id}`);
    };

    const retrievePublicTheme = async (id: number) => {
        return await api.getPublic<Theme>(`themes/${id}`);
    };

    const saveTheme = async (theme: Theme) => {
        if (theme.id <= 0) {
            return await api.post<Theme>('themes', theme);
        } else {
            return await api.put<Theme>(`themes/${theme.id}`, theme);
        }
    };

    const deleteTheme = async (id: number) => {
        return await api.destroy<void>(`themes/${id}`);
    };

    return {
        listThemes,
        listPublicThemes,
        retrieveTheme,
        retrievePublicTheme,
        saveTheme,
        deleteTheme,
    };
}
