import {afterEach, describe, expect, it, vi} from 'vitest';
import {BaseApiService} from '../../../services/base-api-service';
import {FormTriggerApiService} from './form-trigger-api-service';

describe('FormTriggerApiService', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('should request the selected overview mode and search term', async () => {
        const get = vi
            .spyOn(BaseApiService.prototype, 'get')
            .mockResolvedValue({content: []});

        await new FormTriggerApiService().listOverview(
            1,
            24,
            'Drafted',
            'Hundesteuer',
            'id',
            'DESC',
        );

        expect(get).toHaveBeenCalledWith('/api/forms/v1/', {
            query: {
                page: 1,
                size: 24,
                sort: 'id,DESC',
                view: 'Drafted',
                search: 'Hundesteuer',
            },
        });
    });
});
