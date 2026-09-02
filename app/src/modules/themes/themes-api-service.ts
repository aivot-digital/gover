import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {ThemeRequestDTO, ThemeResponseDTO} from './models/theme';
import {DEFAULT_APPEARANCE_COLORS} from '../../theming/resolve-appearance-colors';

interface ThemeFilter {
    name: string;
}

export class ThemesApiService extends CrudApiService<ThemeRequestDTO, ThemeResponseDTO, ThemeResponseDTO, ThemeResponseDTO, ThemeResponseDTO, number, ThemeFilter> {
    public constructor(api: Api) {
        super(api, 'themes/');
    }

    public initialize(): ThemeResponseDTO {
        return {
            id: 0,
            name: '',
            primaryColor: DEFAULT_APPEARANCE_COLORS.primaryColor,
            secondaryColor: DEFAULT_APPEARANCE_COLORS.secondaryColor,
            primaryColorDark: null,
            secondaryColorDark: null,
            faviconKey: null,
            logoKey: null,
            logoKeyDark: null,
        };
    }
}
