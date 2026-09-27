import {afterEach, describe, expect, it, vi} from 'vitest';
import {ProcessNodeApiService} from './process-node-api-service';
import {generateProcessNodeDataKey} from '../utils/process-node-data-key';

afterEach(() => vi.restoreAllMocks());

describe('automatic process node data keys', () => {
    it('creates six lowercase characters beginning with a letter', () => {
        for (let i = 0; i < 100; i++) {
            expect(generateProcessNodeDataKey()).toMatch(/^[a-hjkmnp-tv-z][0-9a-hjkmnp-tv-z]{5}$/);
        }
    });

    it('retries a scoped key collision and returns the persisted key', async () => {
        const api = new ProcessNodeApiService();
        const create = vi
            .spyOn(api, 'create')
            .mockRejectedValueOnce({
                status: 409,
                message: 'Belegt',
                displayableToUser: true,
                details: {reason: 'process_node_data_key_conflict'},
            })
            .mockImplementationOnce(async (node) => ({
                ...node,
                id: 42,
            }));
        const input = {
            ...api.initialize(),
            dataKey: 'source_key',
        };
        const saved = await api.createWithGeneratedDataKey(input);
        expect(create).toHaveBeenCalledTimes(2);
        expect(saved.dataKey).toMatch(/^[a-hjkmnp-tv-z][0-9a-hjkmnp-tv-z]{5}$/);
        expect(saved.dataKey).not.toBe(create.mock.calls[0][0].dataKey);
        expect(input.dataKey).toBe('source_key');
    });

    it('limits collision retries and does not retry unrelated errors', async () => {
        const api = new ProcessNodeApiService();
        const conflict = {
            status: 409,
            message: 'Belegt',
            displayableToUser: true,
            details: {reason: 'process_node_data_key_conflict'},
        };
        const create = vi.spyOn(api, 'create').mockRejectedValue(conflict);
        await expect(api.createWithGeneratedDataKey(api.initialize())).rejects.toBe(conflict);
        expect(create).toHaveBeenCalledTimes(5);
        create.mockClear().mockRejectedValue({
            ...conflict,
            details: null,
        });
        await expect(api.createWithGeneratedDataKey(api.initialize())).rejects.toMatchObject({status: 409});
        expect(create).toHaveBeenCalledTimes(1);
    });
});
