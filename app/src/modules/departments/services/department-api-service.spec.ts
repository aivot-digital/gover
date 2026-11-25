import {describe, expect, it, vi} from 'vitest';
import {DepartmentApiService} from './department-api-service';
import {type PublicDepartmentResponseDTO} from '../entities/v-department-shadowed-entity';

describe('DepartmentApiService.retrievePublic', () => {
    it('shares a pending request for the same department and loads again once it settled', async () => {
        const department = {id: 24, name: 'Bürgerservice'} as PublicDepartmentResponseDTO;
        const get = vi.spyOn(DepartmentApiService.prototype, 'get').mockResolvedValue(department);

        const results = await Promise.all([
            new DepartmentApiService().retrievePublic(24),
            new DepartmentApiService().retrievePublic(24),
            new DepartmentApiService().retrievePublic(25),
        ]);

        expect(results).toEqual([department, department, department]);
        expect(get).toHaveBeenCalledTimes(2);
        expect(get).toHaveBeenCalledWith('/api/public/departments/24/');
        expect(get).toHaveBeenCalledWith('/api/public/departments/25/');

        await new DepartmentApiService().retrievePublic(24);
        expect(get).toHaveBeenCalledTimes(3);
    });
});
