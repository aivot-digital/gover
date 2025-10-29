import {SystemSetupDTO} from './dtos/system-setup-dto';
import {BaseApiService} from '../../services/base-api-service';
import {HealthData} from '../../models/dtos/health-data';
import {HttpExchanges} from '../../models/dtos/http-exchange';

export class SystemApiService extends BaseApiService {
    public fetchSetup(): Promise<SystemSetupDTO> {
        return this.getUnauthenticated<SystemSetupDTO>('/api/public/system/setup/', {
            doNotHandleStatusCodes: true,
        });
    }

    public static getFaviconUrl(): string {
        return '/api/public/system/favicon/';
    }

    public static getLogoUrl(): string {
        return '/api/public/system/logo/';
    }

    public getHealth(): Promise<HealthData> {
        return this.get<HealthData>('/api/public/actuator/health', {
            doNotHandleStatusCodes: true,
        });
    }

    public getHttpExchanges(): Promise<HttpExchanges> {
        return this.get<HttpExchanges>('/api/public/actuator/httpexchanges');
    }

    public getFileExtensions(): Promise<string[]> {
        return this.get<string[]>('system/file-extensions');
    }

    public async testSmtp(email: string)  {
        return await this.post<{targetMail: string}, { result?: string }>('/api/mail/test/', {
            targetMail: email,
        });
    };
}