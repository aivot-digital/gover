import {describe, expect, it, vi} from 'vitest';
import {literalAuthoredValue} from '../../../models/element-data';
import {ProcessInstanceTaskApiService} from './process-instance-task-api-service';

describe('ProcessInstanceTaskApiService', () => {
    it('should append files nested in authored replicating-container rows', async () => {
        const service = new ProcessInstanceTaskApiService();
        let submittedFormData: FormData | undefined;
        vi.spyOn(service, 'putFormData').mockImplementation(async (_path, formData) => {
            submittedFormData = formData;
            return {} as never;
        });
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(new Blob(['content']))));

        await service.putStaffTaskView(10, 20, {
            attachments: literalAuthoredValue([{
                id: 'row-1',
                values: {
                    file: literalAuthoredValue([{
                        name: 'proof.pdf',
                        size: 7,
                        uri: 'blob:proof',
                    }]),
                },
            }]),
        });

        expect(submittedFormData?.getAll('fileUris')).toEqual(['blob:proof']);
        expect(submittedFormData?.getAll('files')).toHaveLength(1);
        expect(submittedFormData?.get('inputs')).toBe(JSON.stringify({
            attachments: literalAuthoredValue([{
                id: 'row-1',
                values: {
                    file: literalAuthoredValue([{
                        name: 'proof.pdf',
                        size: 7,
                        uri: 'blob:proof',
                    }]),
                },
            }]),
        }));
    });
});
