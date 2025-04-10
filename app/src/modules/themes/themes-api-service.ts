import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {Theme} from './models/theme';

interface ThemeFilter {
    name: string;
}

export class ThemesApiService extends CrudApiService<Theme, Theme, Theme, Theme, Theme, number, ThemeFilter> {
    public constructor(api: Api) {
        super(api, 'themes/');
    }

    public initialize(): Theme {
        return {
            id: 0,
            name: '',
            main: '#253B5B',
            mainDark: '#102334',
            accent: '#F8D27C',
            error: '#CD362D',
            info: '#1F7894',
            warning: '#B55E06',
            success: '#378550',
        };
    }
}