import {ApiService} from "./api-service";


class _SystemService {

    public async testSmtp(targetEmail: string): Promise<{ result?: string }> {
        return ApiService.post('system/test-smtp', {
            targetMail: targetEmail,
        });
    }

    public async getSentryDns(): Promise<string[]> {
        return ApiService.get('public/sentry-dns');
    }

    public async getFileExtensions(): Promise<string[]> {
        return ApiService.get('system/file-extensions');
    }

    public async getContentTypes(): Promise<string[]> {
        return ApiService.get('system/content-types');
    }
}

export const SystemService = new _SystemService();
