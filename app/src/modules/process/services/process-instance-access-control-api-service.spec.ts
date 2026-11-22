import {describe, expect, it, vi} from 'vitest';
import {ProcessInstanceAccessControlApiService} from './process-instance-access-control-api-service';

const rule = {
    id: 3,
    sourceDepartmentId: null,
    sourceTeamId: 8,
    targetProcessInstanceId: 17,
    permissions: ['process_instance.read'],
    created: '',
    updated: '',
};

describe('ProcessInstanceAccessControlApiService.replace', () => {
    it('states the loaded rules with their lock versions', async () => {
        const service = new ProcessInstanceAccessControlApiService();
        const put = vi.spyOn(service, 'put').mockResolvedValue([] as never);

        await service.replace(17, [rule], [{...rule, lockVersion: 2}, {...rule, id: 4, lockVersion: 0}]);

        expect(put).toHaveBeenCalledWith(
            '/api/process-instance-access-controls/instance/17/',
            [{sourceDepartmentId: null, sourceTeamId: 8, permissions: ['process_instance.read']}],
            {query: {basedOn: '3:2,4:0'}},
        );
    });

    it('states an empty set when no rules were loaded', async () => {
        const service = new ProcessInstanceAccessControlApiService();
        const put = vi.spyOn(service, 'put').mockResolvedValue([] as never);

        await service.replace(17, [rule], []);

        expect(put.mock.calls[0][2]).toEqual({query: {basedOn: ''}});
    });

    it('leaves out the state when a loaded rule has no lock version', async () => {
        const service = new ProcessInstanceAccessControlApiService();
        const put = vi.spyOn(service, 'put').mockResolvedValue([] as never);

        await service.replace(17, [rule], [rule]);

        expect(put.mock.calls[0][2]).toEqual({query: {basedOn: undefined}});
    });
});
