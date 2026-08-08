import {BaseApiService} from '../../services/base-api-service';

export interface MailConfigurationResponseDTO {
    configured: boolean;
    host: string | null;
    port: number | null;
    authenticationEnabled: boolean;
    username: string | null;
    passwordConfigured: boolean;
    startTlsEnabled: boolean;
    senderName: string | null;
    senderAddress: string | null;
    configurationIssues: string[];
}

export interface TestMailResponseDTO {
    success: boolean;
    errorMessage: string | null;
}

export class MailApiService extends BaseApiService {
    public getConfiguration(): Promise<MailConfigurationResponseDTO> {
        return this.get<MailConfigurationResponseDTO>('/api/mail/configuration/');
    }

    public sendTestMail(targetMail: string): Promise<TestMailResponseDTO> {
        return this.post<{targetMail: string}, TestMailResponseDTO>('/api/mail/test/', {
            targetMail,
        });
    }
}
