import {BaseApiService} from '../../services/base-api-service';
import {HealthData} from '../../models/dtos/health-data';

export class SystemApiService extends BaseApiService {
    public getHealth(): Promise<HealthData> {
        return this.get<HealthData>('/api/actuator/health/', {
            doNotHandleStatusCodes: true,
        });
    }

    /**
     * @deprecated use AppConfig
     */
    public getFileExtensions(): Promise<string[]> {
        return this.get<string[]>('/api/public/system/file-extensions/');
    }

    public async testSmtp(email: string) {
        return await this.post<{ targetMail: string }, { result?: string }>('/api/mail/test/', {
            targetMail: email,
        });
    };
}