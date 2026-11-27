import {describe, expect, it, vi} from 'vitest';
import {PluginsApiService} from './plugins-api-service';

describe('PluginsApiService', () => {
    it('retrieves one plugin by its encoded key and forwards request options', async () => {
        const service = new PluginsApiService();
        const abortController = new AbortController();
        const get = vi.spyOn(service, 'get').mockResolvedValue({key: 'com.example/test'});

        await service.getPlugin('com.example/test', {abort: abortController.signal});

        expect(get).toHaveBeenCalledWith('/api/plugins/com.example%2Ftest/', {
            abort: abortController.signal,
        });
    });
});
