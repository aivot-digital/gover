import {afterEach, describe, expect, it, vi} from 'vitest';
import {ProcessInstanceEventApiService} from './process-instance-event-api-service';

describe('ProcessInstanceEventApiService', () => {
    afterEach(() => vi.restoreAllMocks());

    it.each([true, false, undefined])('sends restart history option %s on the log endpoint', async (includeRestartHistory) => {
        const service = new ProcessInstanceEventApiService();
        const get = vi.spyOn(service, 'get').mockResolvedValue({} as never);
        await service.getEventLog({processInstanceId: 12, processInstanceTaskId: 36, includeRestartHistory});
        const query = get.mock.calls[0][1]!.query;
        const url = new URL(service.createPath('/api/process-instance-events/log/', query), 'https://example.org');
        expect(url.searchParams.get('processInstanceTaskId')).toBe('36');
        expect(url.searchParams.get('includeRestartHistory')).toBe(includeRestartHistory == null ? null : String(includeRestartHistory));
    });

    it.each([true, false, undefined])('sends history relevance %s and concerned references on both endpoints', async (historyRelevant) => {
        const service = new ProcessInstanceEventApiService();
        const get = vi.spyOn(service, 'get').mockResolvedValue({} as never);
        const filters = {
            historyRelevant,
            concernedUserId: 'user-1',
            concernedIdentityId: 'identity-1',
            concernedIdentityTitle: 'Betroffene Person',
        };

        await service.list(0, 50, 'timestamp', 'DESC', {processInstanceId: 12, ...filters});
        expect(get).toHaveBeenLastCalledWith('/api/process-instance-events/', expect.objectContaining({
            query: expect.objectContaining(filters),
        }));

        await service.getEventLog({processInstanceId: 12, search: ' Robin ', filter: 'notable', ...filters});
        expect(get).toHaveBeenLastCalledWith('/api/process-instance-events/log/', expect.objectContaining({
            query: expect.objectContaining({...filters, search: 'Robin', notableOnly: true}),
        }));

        const query = get.mock.calls.at(-1)![1]!.query;
        const url = new URL(service.createPath('/api/process-instance-events/log/', query), 'https://example.org');
        expect(url.searchParams.get('historyRelevant')).toBe(historyRelevant == null ? null : String(historyRelevant));
        expect(url.searchParams.get('concernedIdentityTitle')).toBe('Betroffene Person');
    });

    it('initializes history relevance to false', () => {
        expect(new ProcessInstanceEventApiService().initialize().historyRelevant).toBe(false);
    });
});
