import {ApiService} from './api-service';
import {type Health} from '../models/dtos/health';


class _SystemService {
    public async testSmtp(targetEmail: string): Promise<{result?: string}> {
        return await ApiService.post('system/test-smtp', {
            targetMail: targetEmail,
        });
    }

    public async getSentryDsn(): Promise<string[]> {
        return await ApiService.get('public/sentry-dns');
    }

    public async getFileExtensions(): Promise<string[]> {
        return await ApiService.get('system/file-extensions');
    }

    public async getContentTypes(): Promise<string[]> {
        return await ApiService.get('system/content-types');
    }

    public async checkHealth(): Promise<Health> {
        return await ApiService.get('public/actuator/health');
    }
}

export const SystemService = new _SystemService();
