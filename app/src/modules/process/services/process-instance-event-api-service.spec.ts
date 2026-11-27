import {ProcessHistoryEventScope} from '../enums/process-history-event-scope';
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

    it('loads enriched timeline events with a stable timestamp/id order', async () => {
        const service = new ProcessInstanceEventApiService();
        const get = vi.spyOn(service, 'get').mockResolvedValue({content: [{id: 7, technical: true, audit: false,
            triggeringUserName: 'Ada Beispiel', details: {manualIntervention: true}}], page: {number: 0}} as never);
        const result = await service.listHistory(12, {scope: ProcessHistoryEventScope.ManualInterventions});
        expect(get).toHaveBeenCalledWith('/api/process-instance-events/history/', {query: {
            processInstanceId: 12, scope: ProcessHistoryEventScope.ManualInterventions, processInstanceTaskId: undefined, page: 0, size: 20, sort: ['timestamp,ASC', 'id,ASC'],
        }});
        expect(result.content[0]).toEqual(expect.objectContaining({isTechnical: true, isAudit: false,
            triggeringUserName: 'Ada Beispiel', details: {manualIntervention: true}}));
    });

    it('requests a bounded task history separately from the paginated overview scopes', async () => {
        const service = new ProcessInstanceEventApiService();
        const get = vi.spyOn(service, 'get').mockResolvedValue({content: [], page: {number: 0}} as never);
        await service.listHistory(12, {scope: ProcessHistoryEventScope.Task, taskId: 42, size: 20, sortOrder: 'DESC'});
        expect(get).toHaveBeenLastCalledWith('/api/process-instance-events/history/', {query: {
            processInstanceId: 12, scope: ProcessHistoryEventScope.Task, processInstanceTaskId: 42,
            page: 0, size: 20, sort: ['timestamp,DESC', 'id,DESC'],
        }});
        await service.listHistory(12, {scope: ProcessHistoryEventScope.InstanceErrors, page: 2});
        expect(get).toHaveBeenLastCalledWith('/api/process-instance-events/history/', {query: {
            processInstanceId: 12, scope: ProcessHistoryEventScope.InstanceErrors, processInstanceTaskId: undefined,
            page: 2, size: 20, sort: ['timestamp,ASC', 'id,ASC'],
        }});
    });

    it('initializes history relevance to false', () => {
        expect(new ProcessInstanceEventApiService().initialize().historyRelevant).toBe(false);
    });
});
