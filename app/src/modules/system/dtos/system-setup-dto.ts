import {Theme} from '../../themes/models/theme';

export interface SystemSetupDTO {
    providerName?: string | null;
    providerTheme?: Theme | null;
}