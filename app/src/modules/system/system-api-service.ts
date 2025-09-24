import {SystemSetupDTO} from './dtos/system-setup-dto';
import {BaseApiService} from '../../services/base-api-service';

export class SystemApiService extends BaseApiService {
    public fetchSetup(): Promise<SystemSetupDTO> {
        return this.getUnauthenticated<SystemSetupDTO>('/api/public/system/setup/');
    }

    public static getFaviconUrl(): string {
        return '/api/public/system/favicon/';
    }

    public static getLogoUrl(): string {
        return '/api/public/system/logo/';
    }
}