import {describe, expect, it, vi} from 'vitest';
import {literalAuthoredValue} from '../../../models/element-data';
import {ProcessInstanceTaskApiService} from './process-instance-task-api-service';

describe('ProcessInstanceTaskApiService', () => {
    it('should append only the given files and keep the authored values unchanged', async () => {
        const service = new ProcessInstanceTaskApiService();
        let submittedFormData: FormData | undefined;
        vi.spyOn(service, 'putFormData').mockImplementation(async (_path, formData) => {
            submittedFormData = formData;
            return {} as never;
        });
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(new Blob(['content']))));
        const proof = {
            name: 'proof.pdf',
            size: 7,
            uri: 'blob:proof',
        };
        const payload = {
            attachments: literalAuthoredValue([{
                id: 'row-1',
                values: {
                    file: literalAuthoredValue([proof]),
                    hiddenFile: literalAuthoredValue([{
                        name: 'hidden.pdf',
                        size: 3,
                        uri: 'blob:hidden',
                    }]),
                },
            }]),
        };

        await service.putStaffTaskView(10, 20, payload, [proof]);

        expect(submittedFormData?.getAll('fileUris')).toEqual(['blob:proof']);
        expect(submittedFormData?.getAll('files')).toHaveLength(1);
        expect(submittedFormData?.get('inputs')).toBe(JSON.stringify(payload));
    });
});
