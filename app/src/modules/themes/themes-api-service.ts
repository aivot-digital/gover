import {ThemeRequestDTO, ThemeResponseDTO} from './models/theme';
import {DEFAULT_APPEARANCE_COLORS} from '../../theming/resolve-appearance-colors';
import {BaseCrudApiService} from "../../services/base-crud-api-service";

interface ThemeFilter {
    name: string;
}

export class ThemesApiService extends BaseCrudApiService<
    ThemeRequestDTO,
    ThemeResponseDTO,
    ThemeResponseDTO,
    ThemeResponseDTO,
    number,
    ThemeFilter,
    keyof ThemeResponseDTO
> {
    public constructor() {
        super('/api/themes/');
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
