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

    it('loads runtime identity slots for the selected test form', async () => {
        const identitySlots = [{id: 'applicant'}];
        const get = vi
            .spyOn(BaseApiService.prototype, 'get')
            .mockResolvedValue({identitySlots});

        const result = await new FormTriggerApiService().getIdentitySlots(
            'example process',
            'application/form',
            'test-claim',
        );

        expect(get).toHaveBeenCalledWith('/api/public/form/example%20process/application%2Fform/', {
            query: {'test-claim': 'test-claim'},
            skipAuthCheck: true,
        });
        expect(result).toBe(identitySlots);
    });
});
