import {SystemSetupDTO} from './dtos/system-setup-dto';

export class SystemApiService {
    public fetchSetup(): Promise<SystemSetupDTO> {
        return fetch('/api/public/system/setup/', {
            method: 'GET',
        })
            .then((response) => {
                return response.json();
            });
    }

    public static getFaviconUrl(): string {
        return '/api/public/system/favicon/';
    }

    public static getLogoUrl(): string {
        return '/api/public/system/logo/';
    }
}