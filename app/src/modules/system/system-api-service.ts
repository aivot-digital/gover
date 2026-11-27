import {BaseApiService} from '../../services/base-api-service';
import {HealthData} from '../../models/dtos/health-data';

export interface SystemBuildDTO {
    buildVersion: string | null;
    buildNumber: string | null;
    buildTimestamp: string | null;
}

export class SystemApiService extends BaseApiService {
    public getHealth(): Promise<HealthData> {
        return this.get<HealthData>('/api/system/health/', {
            doNotHandleStatusCodes: true,
        });
    }

    public getBuild(): Promise<SystemBuildDTO> {
        return this.get<SystemBuildDTO>('/api/system/build/');
    }

    /**
     * @deprecated use AppConfig
     */
    public getFileExtensions(): Promise<string[]> {
        return this.get<string[]>('/api/public/system/file-extensions/');
    }
}
